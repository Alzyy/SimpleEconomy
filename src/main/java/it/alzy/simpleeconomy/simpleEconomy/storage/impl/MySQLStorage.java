package it.alzy.simpleeconomy.simpleEconomy.storage.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import it.alzy.simpleeconomy.records.DatabaseInfo;
import it.alzy.simpleeconomy.simpleEconomy.SimpleEconomy;
import it.alzy.simpleeconomy.simpleEconomy.configurations.SettingsConfig;
import it.alzy.simpleeconomy.simpleEconomy.storage.Storage;

public class MySQLStorage implements Storage {

    private final SimpleEconomy plugin;
    private final HikariDataSource dataSource;
    private final Executor executor;

    public MySQLStorage(SimpleEconomy plugin, DatabaseInfo info) {
        this.plugin = plugin;
        this.executor = plugin.getExecutor();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(info.JDBCString());
        config.setUsername(info.username());
        config.setPassword(info.password());
        config.setMaximumPoolSize(info.maxPoolSize());
        config.setConnectionTimeout(5000);
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("SimpleEconomy-MySQL-Pool");

        this.dataSource = new HikariDataSource(config); 

        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement("""
                            CREATE TABLE IF NOT EXISTS players (
                                uuid VARCHAR(36) PRIMARY KEY,
                                balance DOUBLE NOT NULL
                            )
                        """)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe(String.format("Couldn't create table using MySQL: %s", e.getMessage()));
        }
    }

    @Override
    public void save(UUID uuid, double balance) {
        executor.execute(() -> saveSync(uuid, balance));
    }

    @Override
    public void saveSync(UUID uuid, double balance) {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement("""
                            INSERT INTO players (uuid, balance)
                            VALUES (?, ?)
                            ON DUPLICATE KEY UPDATE balance = VALUES(balance)
                        """)) {
            stmt.setString(1, uuid.toString());
            stmt.setDouble(2, balance);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to save data for UUID " + uuid + ": " + e.getMessage());
        }
    }

    @Override
    public CompletableFuture<Double> load(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement stmt = conn.prepareStatement("SELECT balance FROM players WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getDouble("balance");
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to load data for UUID " + uuid + ": " + e.getMessage());
            }
            return 0.0;
        }, executor);
    }

    @Override
    public void create(UUID uuid) {
        executor.execute(() -> {
            double balance = SettingsConfig.getInstance().startingBalance();
            plugin.getCacheMap().put(uuid, balance);
            save(uuid, balance);
        });
    }

    public void bulkSaveAndShutdown() {
        var futures = plugin.getCacheMap().entrySet().stream()
                .map(entry -> CompletableFuture.runAsync(() -> saveSync(entry.getKey(), entry.getValue()), executor))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Override
    public CompletableFuture<Boolean> hasAccount(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement stmt = conn.prepareStatement("SELECT balance FROM players WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to load data for UUID " + uuid + ": " + e.getMessage());
                return false;
            }
        }, plugin.getExecutor());
    }
}
