package it.alzy.simpleeconomy.simpleEconomy.storage.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.*;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;

import org.bukkit.Bukkit;

import it.alzy.simpleeconomy.simpleEconomy.SimpleEconomy;
import it.alzy.simpleeconomy.simpleEconomy.configurations.SettingsConfig;
import it.alzy.simpleeconomy.simpleEconomy.storage.Storage;

public class SQLiteStorage implements Storage {

    private final SimpleEconomy plugin;
    private final ExecutorService executor;
    private final HikariDataSource dataSource;

    public SQLiteStorage(SimpleEconomy plugin) {
        this.plugin = plugin;
        this.executor = plugin.getExecutor();

        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File dbFile = new File(dataFolder, "players.db");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getPath());
        config.setMaximumPoolSize(10);
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("SimpleEconomy-SQLite-Pool");
        config.setConnectionTimeout(5000);

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
                            uuid TEXT PRIMARY KEY,
                            balance REAL DEFAULT 0
                        );
                    """;

            try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Couldn't create users table. Disabling plugin.", e);
                Bukkit.getPluginManager().disablePlugin(plugin);
            }
        });
    }

    private void saveToDatabase(UUID uuid, double balance) {
        String sql = """
                    INSERT INTO users(uuid, balance) VALUES (?, ?)
                    ON CONFLICT(uuid) DO UPDATE SET balance = ?;
                """;

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setDouble(2, balance);
            ps.setDouble(3, balance);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save balance for UUID: " + uuid, e);
        }
    }

    @Override
    public void save(UUID uuid, double balance) {
        executor.execute(() -> saveToDatabase(uuid, balance));
    }

    @Override
    public void saveSync(UUID uuid, double balance) {
        if (Bukkit.isPrimaryThread()) {
            plugin.getLogger().severe("[WARNING] saveSync() was called on the main thread! UUID: " + uuid);
        }
        saveToDatabase(uuid, balance);
    }

    public CompletableFuture<Void> saveAsync(UUID uuid, double balance) {
        return CompletableFuture.runAsync(() -> saveToDatabase(uuid, balance), executor);
    }

    @Override
    public CompletableFuture<Double> load(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT balance FROM users WHERE uuid = ?";

            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    double balance = rs.getDouble("balance");
                    plugin.getCacheMap().put(uuid, balance);
                    return balance;
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load balance for UUID " + uuid, e);
            }

            double defaultBalance = SettingsConfig.getInstance().startingBalance();
            plugin.getCacheMap().put(uuid, defaultBalance);
            saveToDatabase(uuid, defaultBalance);
            return defaultBalance;
        }, executor);
    }

    @Override
    public void create(UUID uuid) {
        executor.execute(() -> {
            double balance = SettingsConfig.getInstance().startingBalance();
            plugin.getCacheMap().put(uuid, balance);
            saveToDatabase(uuid, balance);
        });
    }

    public void bulkSaveAndShutdown() {
        List<CompletableFuture<Void>> futures = plugin.getCacheMap().entrySet().stream()
                .map(entry -> saveAsync(entry.getKey(), entry.getValue()))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join(); 
        plugin.getCacheMap().clear();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
