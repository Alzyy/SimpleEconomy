package it.alzy.simpleeconomy.plugin.storage.impl;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.configurations.SettingsConfig;
import it.alzy.simpleeconomy.plugin.records.DatabaseInfo;
import it.alzy.simpleeconomy.plugin.storage.Storage;

public class MySQLStorage implements Storage {

    private final SimpleEconomy plugin;
    private final HikariDataSource dataSource;
    private final Executor executor;
    private final String tableName;

    public MySQLStorage(SimpleEconomy plugin, DatabaseInfo info) {
        this.plugin = plugin;
        this.executor = plugin.getExecutor();
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
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "balance DOUBLE NOT NULL, " +
                "last_seen BIGINT NOT NULL" +
                ")";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Couldn't create table using MySQL: " + e.getMessage());
        }
    }

    @Override
    public void save(UUID uuid, double balance) {
        executor.execute(() -> saveSync(uuid, balance));
    }

    @Override
    public void saveSync(UUID uuid, double balance) {
        String sql = "INSERT INTO `" + tableName + "` (uuid, balance, last_seen) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE balance = VALUES(balance), last_seen = VALUES(last_seen)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setDouble(2, balance);
            stmt.setLong(3, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to save data for UUID " + uuid + ": " + e.getMessage());
        }
    }

    @Override
    public CompletableFuture<Double> getBalance(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT balance FROM `" + tableName + "` WHERE uuid = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getDouble("balance");
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to load data for UUID " + uuid + ": " + e.getMessage());
            }
            return SettingsConfig.getInstance().startingBalance();
        }, executor);
    }

    @Override
    public CompletableFuture<Double> load(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT balance FROM `" + tableName + "` WHERE uuid = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        double balance = rs.getDouble("balance");
                        plugin.getCacheMap().put(uuid, balance);
                        return balance;
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to load data for UUID " + uuid + ": " + e.getMessage());
            }

            double defaultBalance = SettingsConfig.getInstance().startingBalance();
            plugin.getCacheMap().put(uuid, defaultBalance);
            saveSync(uuid, defaultBalance);
            return defaultBalance;
        }, executor);
    }

    @Override
    public void create(UUID uuid) {
        executor.execute(() -> {
            double balance = SettingsConfig.getInstance().startingBalance();
            plugin.getCacheMap().put(uuid, balance);
            saveSync(uuid, balance);
        });
    }

    @Override
    public void bulkSave() {
        var futures = plugin.getCacheMap().entrySet().stream()
                .map(entry -> CompletableFuture.runAsync(() -> saveSync(entry.getKey(), entry.getValue()), executor))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
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
            String sql = "SELECT 1 FROM `" + tableName + "` WHERE uuid = ?";
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
        }, executor);
    }

    @Override
    public Map<String, Double> getTopBalances(int limit) {
        Map<String, Double> topBalances = new LinkedHashMap<>();
        String sql = "SELECT uuid, balance FROM " + tableName + " ORDER BY balance DESC LIMIT ?";

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String uuid = rs.getString("uuid");
                double balance = rs.getDouble("balance");
                topBalances.put(uuid, balance);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to retrieve top balances", e);
        }

        return topBalances;
    }

    @Override
    public void purge(int days) {
        plugin.getExecutor().execute(() -> {
            String sql = "DELETE FROM `" + tableName + "` WHERE last_seen < ?";
            long cutoff = System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L);

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, cutoff);
                int deletedRows = stmt.executeUpdate();
                plugin.getLogger().info("Purged " + deletedRows + " accounts inactive for more than " + days + " days.");
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to purge inactive accounts: " + e.getMessage());
            }
        });
    }

    @Override
    public Map<String, Double> getAllBalances() {
        Map<String, Double> allBalances = new LinkedHashMap<>();
        String sql = "SELECT uuid, balance FROM " + tableName;

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String uuid = rs.getString("uuid");
                double balance = rs.getDouble("balance");
                allBalances.put(uuid, balance);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to retrieve all balances", e);
        }

        return allBalances;
    }
}