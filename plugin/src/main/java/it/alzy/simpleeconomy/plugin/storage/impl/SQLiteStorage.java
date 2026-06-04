package it.alzy.simpleeconomy.plugin.storage.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.configurations.SettingsConfig;
import it.alzy.simpleeconomy.plugin.storage.Storage;
import org.bukkit.Bukkit;

import java.io.File;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class SQLiteStorage implements Storage {

    private final SimpleEconomy plugin;
    private final ExecutorService executor;
    private final HikariDataSource dataSource;

    public SQLiteStorage(SimpleEconomy plugin) {
        this.plugin = plugin;
        this.executor = Executors.newSingleThreadExecutor(t -> new Thread(t, "SimpleEconomy-SQLite-Executor"));

        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File dbFile = new File(dataFolder, "players.db");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getPath());

        config.setMaximumPoolSize(1);
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("SimpleEconomy-SQLite-Pool");
        config.setConnectionTimeout(5000);

        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");
        config.addDataSourceProperty("busy_timeout", "5000");

        this.dataSource = new HikariDataSource(config);
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void init() {
        plugin.getLogger().info("Using SQLITE with HikariCP as storage system!\nCreating table!");

        executor.execute(() -> {
            String sql = """
                    CREATE TABLE IF NOT EXISTS users (
                        uuid TEXT,
                        currency TEXT DEFAULT 'money',
                        balance REAL DEFAULT 0,
                        last_seen BIGINT NOT NULL,
                        PRIMARY KEY (uuid, currency)
                    );
                    """;

            try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Couldn't create users table. Disabling instance.", e);
                Bukkit.getPluginManager().disablePlugin(plugin);
            }
        });
    }

    private void saveToDatabase(UUID uuid, String currency, double balance) {
        String sql = """
                INSERT INTO users(uuid, currency, balance, last_seen) VALUES (?, ?, ?, ?)
                ON CONFLICT(uuid, currency) DO UPDATE SET balance = ?, last_seen = ?;
                """;

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            long timestamp = System.currentTimeMillis();

            ps.setString(1, uuid.toString());
            ps.setString(2, currency);
            ps.setDouble(3, balance);
            ps.setLong(4, timestamp);

            ps.setDouble(5, balance);
            ps.setLong(6, timestamp);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save balance for UUID: " + uuid + " and currency: " + currency, e);
        }
    }

    @Override
    public void save(UUID uuid, String currency, double balance) {
        executor.execute(() -> saveToDatabase(uuid, currency, balance));
    }

    @Override
    public void saveSync(UUID uuid, String currency, double balance) {
        if (Bukkit.isPrimaryThread()) {
            plugin.getLogger().severe("[WARNING] saveSync() was called on the main thread! UUID: " + uuid);
        }
        saveToDatabase(uuid, currency, balance);
    }

    @Override
    public CompletableFuture<Map<String, Double>> load(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT currency, balance FROM users WHERE uuid = ?";
            Map<String, Double> balances = new ConcurrentHashMap<>();

            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();

                while (rs.next()) {
                    balances.put(rs.getString("currency"), rs.getDouble("balance"));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load balances for UUID " + uuid, e);
            }

            if (balances.isEmpty()) {
                String defaultCurrency = "money";
                double defaultBalance = SettingsConfig.getInstance().startingBalance();
                balances.put(defaultCurrency, defaultBalance);
                saveToDatabase(uuid, defaultCurrency, defaultBalance);
            }

            plugin.getCache().put(uuid, balances);
            return balances;
        }, executor);
    }

    @Override
    public CompletableFuture<Double> getBalance(UUID uuid, String currency) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT balance FROM users WHERE uuid = ? AND currency = ?";

            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, currency);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    return rs.getDouble("balance");
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load balance for UUID " + uuid + " and currency " + currency, e);
            }

            return SettingsConfig.getInstance().startingBalance();
        }, executor);
    }

    @Override
    public void create(UUID uuid) {
        executor.execute(() -> {
            String defaultCurrency = "money";
            double balance = SettingsConfig.getInstance().startingBalance();

            Map<String, Double> balances = new ConcurrentHashMap<>();
            balances.put(defaultCurrency, balance);

            plugin.getCache().put(uuid, balances);
            saveToDatabase(uuid, defaultCurrency, balance);
        });
    }

    @Override
    public void bulkSave() {
        Set<UUID> dirtyPlayers = plugin.getCache().getAndClearDirtyPlayers();
        if (dirtyPlayers.isEmpty()) return;

        String sql = """
                INSERT INTO users(uuid, currency, balance, last_seen) VALUES (?, ?, ?, ?)
                ON CONFLICT(uuid, currency) DO UPDATE SET balance = ?, last_seen = ?;
                """;

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                long timestamp = System.currentTimeMillis();

                for (UUID uuid : dirtyPlayers) {
                    Map<String, Double> balances = plugin.getCache().get(uuid);
                    if (balances == null) continue;

                    String uuidStr = uuid.toString();
                    for (Map.Entry<String, Double> currencyEntry : balances.entrySet()) {
                        ps.setString(1, uuidStr);
                        ps.setString(2, currencyEntry.getKey());
                        ps.setDouble(3, currencyEntry.getValue());
                        ps.setLong(4, timestamp);

                        ps.setDouble(5, currencyEntry.getValue());
                        ps.setLong(6, timestamp);
                        ps.addBatch();
                    }
                }

                ps.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                plugin.getLogger().log(Level.SEVERE, "Failed to execute bulk save", e);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Database connection error during bulk save", e);
        }
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    @Override
    public CompletableFuture<Boolean> hasAccount(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM users WHERE uuid = ? LIMIT 1")) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to load data for UUID " + uuid + ": " + e.getMessage());
                return false;
            }
        }, executor);
    }

    @Override
    public Map<String, Double> getTopBalances(String currency, int limit) {
        Map<String, Double> topBalances = new LinkedHashMap<>();
        String sql = "SELECT uuid, balance FROM users WHERE currency = ? ORDER BY balance DESC LIMIT ?";

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, currency);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String uuid = rs.getString("uuid");
                double balance = rs.getDouble("balance");
                topBalances.put(uuid, balance);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to retrieve top balances for currency: " + currency, e);
        }

        return topBalances;
    }

    @Override
    public void purge(int days) {
        executor.execute(() -> {
            String sql = "DELETE FROM users WHERE last_seen < ?";
            long cutoff = System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L);

            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, cutoff);
                int rowsDeleted = ps.executeUpdate();

                if (rowsDeleted > 0) {
                    plugin.getLogger().info("Purged " + rowsDeleted + " entries inactive for more than " + days + " days.");
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to purge inactive accounts", e);
            }
        });
    }

    @Override
    public Map<String, Double> getAllBalances(String currency) {
        Map<String, Double> allBalances = new LinkedHashMap<>();
        String sql = "SELECT uuid, balance FROM users WHERE currency = ?";

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, currency);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String uuid = rs.getString("uuid");
                double balance = rs.getDouble("balance");
                allBalances.put(uuid, balance);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to retrieve all balances for currency: " + currency, e);
        }

        return allBalances;
    }
}