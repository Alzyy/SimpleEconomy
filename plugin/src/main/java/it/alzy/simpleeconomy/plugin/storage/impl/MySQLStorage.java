package it.alzy.simpleeconomy.plugin.storage.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.configurations.SettingsConfig;
import it.alzy.simpleeconomy.plugin.records.DatabaseInfo;
import it.alzy.simpleeconomy.plugin.storage.Storage;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class MySQLStorage implements Storage {

    private final SimpleEconomy plugin;
    private final HikariDataSource dataSource;
    private final String tableName;

    public MySQLStorage(SimpleEconomy plugin, DatabaseInfo info) {
        this.plugin = plugin;
        this.tableName = info.tablePrefix() + "players";

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(info.JDBCString());
        config.setUsername(info.username());
        config.setPassword(info.password());
        config.setMaximumPoolSize(info.maxPoolSize());
        config.setConnectionTimeout(5000);
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("SimpleEconomy-MySQL-Pool");

        this.dataSource = new HikariDataSource(config);

        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS `" + tableName + "` (" +
                "uuid VARCHAR(36) NOT NULL, " +
                "currency VARCHAR(64) NOT NULL DEFAULT 'money', " +
                "balance DOUBLE NOT NULL, " +
                "last_seen BIGINT NOT NULL, " +
                "PRIMARY KEY (uuid, currency)" +
                ")";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Couldn't create table using MySQL: " + e.getMessage());
        }
    }

    @Override
    public void save(UUID uuid, String currency, double balance) {
        plugin.runAsync(() -> saveSync(uuid, currency, balance));
    }

    @Override
    public void saveSync(UUID uuid, String currency, double balance) {
        if (Bukkit.isPrimaryThread()) {
            plugin.getLogger().severe("[WARNING] saveSync() was called on the main thread! UUID: " + uuid);
        }

        String sql = "INSERT INTO `" + tableName + "` (uuid, currency, balance, last_seen) VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE balance = VALUES(balance), last_seen = VALUES(last_seen)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, currency);
            stmt.setDouble(3, balance);
            stmt.setLong(4, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to save data for UUID " + uuid + " and currency " + currency + ": " + e.getMessage());
        }
    }

    @Override
    public CompletableFuture<Double> getBalance(UUID uuid, String currency) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT balance FROM `" + tableName + "` WHERE uuid = ? AND currency = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, currency);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getDouble("balance");
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to load data for UUID " + uuid + " and currency " + currency + ": " + e.getMessage());
            }
            return SettingsConfig.getInstance().startingBalance();
        }, plugin.getExecutor());
    }

    @Override
    public CompletableFuture<Map<String, Double>> load(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT currency, balance FROM `" + tableName + "` WHERE uuid = ?";
            Map<String, Double> balances = new ConcurrentHashMap<>();

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        balances.put(rs.getString("currency"), rs.getDouble("balance"));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to load data for UUID " + uuid + ": " + e.getMessage());
            }

            if (balances.isEmpty()) {
                String defaultCurrency = "money";
                double defaultBalance = SettingsConfig.getInstance().startingBalance();
                balances.put(defaultCurrency, defaultBalance);
                saveSync(uuid, defaultCurrency, defaultBalance);
            }

            plugin.getCache().put(uuid, balances);
            return balances;
        }, plugin.getExecutor());
    }

    @Override
    public void create(UUID uuid) {
        plugin.runAsync(() -> {
            String defaultCurrency = "money";
            double balance = SettingsConfig.getInstance().startingBalance();

            Map<String, Double> balances = new ConcurrentHashMap<>();
            balances.put(defaultCurrency, balance);

            plugin.getCache().put(uuid, balances);
            saveSync(uuid, defaultCurrency, balance);
        });
    }

    @Override
    public void bulkSave() {
        Set<UUID> dirtyPlayers = plugin.getCache().getAndClearDirtyPlayers();
        if (dirtyPlayers.isEmpty()) return;

        String sql = "INSERT INTO `" + tableName + "` (uuid, currency, balance, last_seen) VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE balance = VALUES(balance), last_seen = VALUES(last_seen)";

        try (Connection conn = dataSource.getConnection()) {
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
    }

    @Override
    public CompletableFuture<Boolean> hasAccount(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT 1 FROM `" + tableName + "` WHERE uuid = ? LIMIT 1";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to check account for UUID " + uuid + ": " + e.getMessage());
                return false;
            }
        }, plugin.getExecutor());
    }

    @Override
    public Map<String, Double> getTopBalances(String currency, int limit) {
        Map<String, Double> topBalances = new LinkedHashMap<>();
        String sql = "SELECT uuid, balance FROM `" + tableName + "` WHERE currency = ? ORDER BY balance DESC LIMIT ?";

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
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
        plugin.runAsync(() -> {
            String sql = "DELETE FROM `" + tableName + "` WHERE last_seen < ?";
            long cutoff = System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L);

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, cutoff);
                int deletedRows = stmt.executeUpdate();

                if (deletedRows > 0) {
                    plugin.getLogger().info("Purged " + deletedRows + " entries inactive for more than " + days + " days.");
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to purge inactive accounts: " + e.getMessage());
            }
        });
    }

    @Override
    public Map<String, Double> getAllBalances(String currency) {
        Map<String, Double> allBalances = new LinkedHashMap<>();
        String sql = "SELECT uuid, balance FROM `" + tableName + "` WHERE currency = ?";

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
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