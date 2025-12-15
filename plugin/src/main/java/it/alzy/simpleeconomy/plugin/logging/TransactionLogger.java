package it.alzy.simpleeconomy.plugin.logging;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import it.alzy.simpleeconomy.api.TransactionTypes;
import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.configurations.SettingsConfig;
import it.alzy.simpleeconomy.plugin.records.Transaction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;

public class TransactionLogger {

    private final SimpleEconomy plugin;
    private HikariDataSource dataSource;

    public TransactionLogger(SimpleEconomy plugin) {
        this.plugin = plugin;
    }

    public void init() {
        plugin.getExecutor().execute(() -> {
            File folder = plugin.getDataFolder();
            if(!folder.exists()) folder.mkdirs();

            File dbFile = new File(folder, "transactions.db");
            try {
                if(dbFile.exists() && Files.size(dbFile.toPath()) > MBToBytes(SettingsConfig.getInstance().getTransactionLoggingMaxFileSize())) {
                    plugin.getLogger().info("Transaction log file exceeded max size, rotating it...");
                    File rotatedFile = new File(folder, "transactions_" + System.currentTimeMillis() + ".db");
                    if(dbFile.renameTo(rotatedFile)) {
                        plugin.getLogger().info("Transaction log file rotated to " + rotatedFile.getName());
                    } else {
                        plugin.getLogger().warning("Failed to rotate transaction log file!");
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Could not check database size: " + e.getMessage());
            }

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setPoolName("SimpleEconomy-LogPool");
            config.setMaximumPoolSize(1);
            config.setConnectionTimeout(5000);
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            dataSource = new HikariDataSource(config);

            try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL;");
                stmt.execute("PRAGMA synchronous=NORMAL;");

                String sql = """
                        CREATE TABLE IF NOT EXISTS transactions (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            timestamp INTEGER NOT NULL,
                            player_uuid VARCHAR(36) NOT NULL,
                            type VARCHAR(32) NOT NULL,
                            amount REAL NOT NULL,
                            balance_before REAL NOT NULL,
                            balance_after REAL NOT NULL,
                            target_uuid VARCHAR(36) NOT NULL
                        );
                        """;
                stmt.execute(sql);

                stmt.execute("CREATE INDEX IF NOT EXISTS idx_player_uuid ON transactions(player_uuid);");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_timestamp ON transactions(timestamp);");

                plugin.getLogger().info("Transaction logging system initialized.");
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to initialize transaction logging system: " + e.getMessage());
            }
        });
    }

    public void appendLog(Transaction transaction) {
        if(dataSource == null || dataSource.isClosed()) return;
        plugin.getExecutor().execute(() -> {
            String sql = "INSERT INTO transactions (timestamp, player_uuid, type, amount, balance_before, balance_after, target_uuid) VALUES (?, ?, ?, ?, ?, ?, ?);";
            try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, transaction.timestamp());
                pstmt.setString(2, transaction.uuid());
                pstmt.setString(3, transaction.type().name());
                pstmt.setDouble(4, transaction.amount());
                pstmt.setDouble(5, transaction.balanceBefore());
                pstmt.setDouble(6, transaction.balanceAfter());
                pstmt.setString(7, transaction.targetUUID());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to log transaction: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<LinkedList<Transaction>> getHistoryOfPlayer(String uuid, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            LinkedList<Transaction> history = new LinkedList<>();
            if (dataSource == null || dataSource.isClosed()) return history;

            String sql = "SELECT * FROM transactions WHERE player_uuid = ? ORDER BY timestamp DESC LIMIT ?";

            try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, uuid);
                pstmt.setInt(2, limit);

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        history.add(new Transaction(
                                rs.getString("player_uuid"),
                                rs.getString("target_uuid"),
                                rs.getDouble("amount"),
                                rs.getDouble("balance_before"),
                                rs.getDouble("balance_after"),
                                TransactionTypes.valueOf(rs.getString("type").toUpperCase()),
                                rs.getLong("timestamp")
                        ));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to fetch history: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Failed to parse transaction type from DB: " + e.getMessage());
            }
            return history;
        });
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private double MBToBytes(double mb) {
        return mb * 1024 * 1024;
    }
}