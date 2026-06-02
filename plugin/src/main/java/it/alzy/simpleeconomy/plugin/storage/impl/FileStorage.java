package it.alzy.simpleeconomy.plugin.storage.impl;

import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.configurations.SettingsConfig;
import it.alzy.simpleeconomy.plugin.storage.Storage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

public record FileStorage(File folder, SimpleEconomy plugin) implements Storage {

    public FileStorage(File folder, SimpleEconomy plugin) {
        this.plugin = plugin;


        this.folder = new File(folder, "player_data");
        plugin.getLogger().info("Using FileStorage as storage system!");

        if (!this.folder.exists() && !this.folder.mkdirs()) {
            plugin.getLogger().severe("Could not create player_data directory!");
        }
    }

    @Override
    public void save(UUID uuid, String currency, double balance) {
        plugin.runAsync(() -> saveSync(uuid, currency, balance));
    }

    @Override
    public void saveSync(UUID uuid, String currency, double balance) {
        if (plugin.getServer().isPrimaryThread()) {
            plugin.getLogger().severe("[WARNING] saveSync() was called on the main thread! UUID: " + uuid);
        }

        File file = new File(folder, uuid.toString() + ".yml");
        YamlConfiguration config = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();

        config.set("balances." + currency, balance);
        config.set("last_seen", System.currentTimeMillis());

        if (config.contains("balance")) {
            config.set("balance", null);
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save balance for " + uuid, e);
        }
    }

    @Override
    public CompletableFuture<Map<String, Double>> load(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            File file = new File(folder, uuid.toString() + ".yml");
            Map<String, Double> balances = new ConcurrentHashMap<>();
            String defaultCurrency = "money";
            double startingBalance = SettingsConfig.getInstance().startingBalance();

            if (!file.exists()) {
                balances.put(defaultCurrency, startingBalance);
                plugin.getCache().put(uuid, balances);
                saveSync(uuid, defaultCurrency, startingBalance);
                return balances;
            }

            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                boolean requiresSave = false;

                if (config.contains("balance") && !config.contains("balances")) {
                    double legacyBalance = config.getDouble("balance");
                    config.set("balance", null);
                    config.set("balances." + defaultCurrency, legacyBalance);
                    requiresSave = true;
                }

                ConfigurationSection section = config.getConfigurationSection("balances");
                if (section != null) {
                    for (String key : section.getKeys(false)) {
                        balances.put(key, section.getDouble(key));
                    }
                }

                if (balances.isEmpty()) {
                    balances.put(defaultCurrency, startingBalance);
                    config.set("balances." + defaultCurrency, startingBalance);
                    requiresSave = true;
                }

                if (requiresSave) {
                    config.save(file);
                }

                plugin.getCache().put(uuid, balances);
                return balances;

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load balance for " + uuid, e);
                balances.put(defaultCurrency, 0d);
                plugin.getCache().put(uuid, balances);
                return balances;
            }
        }, plugin.getExecutor());
    }

    @Override
    public CompletableFuture<Double> getBalance(UUID uuid, String currency) {
        return CompletableFuture.supplyAsync(() -> {
            File file = new File(folder, uuid.toString() + ".yml");
            double startingBalance = SettingsConfig.getInstance().startingBalance();

            if (!file.exists()) {
                return startingBalance;
            }

            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

                if (config.contains("balances." + currency)) {
                    return config.getDouble("balances." + currency);
                } else if ("money".equals(currency) && config.contains("balance")) {
                    return config.getDouble("balance");
                }

                return startingBalance;
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to get balance for " + uuid, e);
                return 0d;
            }
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

        long timestamp = System.currentTimeMillis();
        var futures = dirtyPlayers.stream()
                .map(uuid -> CompletableFuture.runAsync(() -> {
                    Map<String, Double> balances = plugin.getCache().get(uuid);
                    if (balances == null) return;

                    File file = new File(folder, uuid.toString() + ".yml");
                    YamlConfiguration config = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();

                    for (Map.Entry<String, Double> currencyEntry : balances.entrySet()) {
                        config.set("balances." + currencyEntry.getKey(), currencyEntry.getValue());
                    }

                    config.set("last_seen", timestamp);

                    if (config.contains("balance")) {
                        config.set("balance", null);
                    }

                    try {
                        config.save(file);
                    } catch (IOException e) {
                        plugin.getLogger().log(Level.SEVERE, "Failed to execute bulk save for " + uuid, e);
                    }
                }, plugin.getExecutor()))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    @Override
    public void close() {
        // Nothing to close for file storage
    }

    @Override
    public Map<String, Double> getTopBalances(String currency, int limit) {
        try {
            var task = plugin.getExecutor().submit(() -> {
                Map<String, Double> balances = new HashMap<>();

                File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
                if (files != null) {
                    for (File file : files) {
                        try {
                            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                            double balance = SettingsConfig.getInstance().startingBalance();

                            if (config.contains("balances." + currency)) {
                                balance = config.getDouble("balances." + currency);
                            } else if ("money".equals(currency) && config.contains("balance")) {
                                balance = config.getDouble("balance");
                            }

                            String uuid = file.getName().replace(".yml", "");
                            balances.put(uuid, balance);
                        } catch (Exception e) {
                            plugin.getLogger().log(Level.WARNING, "Failed to read balance from " + file.getName(), e);
                        }
                    }
                }

                return balances.entrySet().stream()
                        .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                        .limit(limit)
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (a, b) -> a,
                                LinkedHashMap::new
                        ));
            });
            return task.get();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get top balances for currency: " + currency, e);
            return new LinkedHashMap<>();
        }
    }

    @Override
    public Map<String, Double> getAllBalances(String currency) {
        try {
            var task = plugin.getExecutor().submit(() -> {
                Map<String, Double> balances = new HashMap<>();

                File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
                if (files != null) {
                    for (File file : files) {
                        try {
                            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                            double balance = SettingsConfig.getInstance().startingBalance();

                            if (config.contains("balances." + currency)) {
                                balance = config.getDouble("balances." + currency);
                            } else if ("money".equals(currency) && config.contains("balance")) {
                                balance = config.getDouble("balance");
                            }

                            String uuid = file.getName().replace(".yml", "");
                            balances.put(uuid, balance);
                        } catch (Exception e) {
                            plugin.getLogger().log(Level.WARNING, "Failed to read balance from " + file.getName(), e);
                        }
                    }
                }

                return balances;
            });
            return task.get();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get all balances for currency: " + currency, e);
            return new HashMap<>();
        }
    }

    @Override
    public void purge(int days) {
        plugin.getExecutor().execute(() -> {
            File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files != null) {
                for (File file : files) {
                    try {
                        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                        long lastSeen = config.getLong("last_seen", System.currentTimeMillis());
                        long cutoff = System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L);

                        if (lastSeen < cutoff) {
                            if (file.delete()) {
                                plugin.getLogger().info("Deleted inactive player data file: " + file.getName());
                            } else {
                                plugin.getLogger().warning("Failed to delete inactive player data file: " + file.getName());
                            }
                        }
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Failed to purge file " + file.getName(), e);
                    }
                }
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> hasAccount(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            File file = new File(folder, uuid.toString() + ".yml");
            return file.exists();
        }, plugin.getExecutor());
    }
}