package it.alzy.simpleeconomy.simpleEconomy.storage.impl;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;

import org.bukkit.configuration.file.YamlConfiguration;

import it.alzy.simpleeconomy.simpleEconomy.SimpleEconomy;
import it.alzy.simpleeconomy.simpleEconomy.configurations.SettingsConfig;
import it.alzy.simpleeconomy.simpleEconomy.storage.Storage;

public class FileStorage implements Storage {

    private final File folder;
    private final SimpleEconomy plugin;
    private final ExecutorService executor;

    public FileStorage(File dataFolder, SimpleEconomy plugin) {
        this.plugin = plugin;
        this.executor = plugin.getExecutor();

        this.folder = new File(dataFolder, "player_data");
        plugin.getLogger().info("Using FileStorage as storage system!");

        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().severe("Could not create player_data directory!");
        }
    }

    private void writeBalanceToFile(UUID uuid, double balance) {
        File file = new File(folder, uuid.toString() + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        config.set("balance", balance);
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save balance for " + uuid, e);
        }
    }

    @Override
    public void save(UUID uuid, double balance) {
        executor.execute(() -> writeBalanceToFile(uuid, balance));
    }

    @Override
    public void saveSync(UUID uuid, double balance) {
        if (plugin.getServer().isPrimaryThread()) {
            plugin.getLogger().severe("[WARNING] saveSync() was called on the main thread! UUID: " + uuid);
        }
        writeBalanceToFile(uuid, balance);
    }

    public CompletableFuture<Void> saveAsync(UUID uuid, double balance) {
        return CompletableFuture.runAsync(() -> writeBalanceToFile(uuid, balance), executor);
    }

    @Override
    public CompletableFuture<Double> load(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            File file = new File(folder, uuid.toString() + ".yml");
            double startingBalance = SettingsConfig.getInstance().startingBalance();

            if (!file.exists()) {
                plugin.getCacheMap().put(uuid, startingBalance);
                writeBalanceToFile(uuid, startingBalance);
                return startingBalance;
            }

            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                double balance = config.getDouble("balance", startingBalance);
                plugin.getCacheMap().put(uuid, balance);
                return balance;
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load balance for " + uuid, e);
                plugin.getCacheMap().put(uuid, 0d);
                return 0d;
            }
        }, executor);
    }

    @Override
    public void create(UUID uuid) {
        double balance = SettingsConfig.getInstance().startingBalance();
        plugin.getCacheMap().put(uuid, balance);
        executor.execute(() -> writeBalanceToFile(uuid, balance));
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
        // no need
    }

    @Override
    public CompletableFuture<Boolean> hasAccount(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            File file = new File(folder, uuid.toString() + ".yml");
            return file.exists();
        }, plugin.getExecutor());
    }
}
