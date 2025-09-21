package it.alzy.simpleeconomy.simpleEconomy;

import co.aikar.commands.PaperCommandManager;
import com.google.common.collect.Maps;

import it.alzy.simpleeconomy.simpleEconomy.api.PAPIExpansion;
import it.alzy.simpleeconomy.simpleEconomy.commands.*;
import it.alzy.simpleeconomy.simpleEconomy.configurations.LangConfig;
import it.alzy.simpleeconomy.simpleEconomy.configurations.SettingsConfig;
import it.alzy.simpleeconomy.simpleEconomy.events.PlayerListener;
import it.alzy.simpleeconomy.simpleEconomy.events.VoucherEvents;
import it.alzy.simpleeconomy.simpleEconomy.records.DatabaseInfo;
import it.alzy.simpleeconomy.simpleEconomy.storage.Storage;
import it.alzy.simpleeconomy.simpleEconomy.storage.impl.FileStorage;
import it.alzy.simpleeconomy.simpleEconomy.storage.impl.MySQLStorage;
import it.alzy.simpleeconomy.simpleEconomy.storage.impl.SQLiteStorage;
import it.alzy.simpleeconomy.simpleEconomy.tasks.AutoSaveTask;
import it.alzy.simpleeconomy.simpleEconomy.tasks.BalTopRefreshTask;
import it.alzy.simpleeconomy.simpleEconomy.utils.*;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.concurrent.*;

public final class SimpleEconomy extends JavaPlugin {

    @Getter
    private static SimpleEconomy instance;

    @Getter
    private ConcurrentMap<UUID, Double> cacheMap;
    @Getter @Setter
    private ConcurrentMap<String, Double> topMap;
    @Getter
    private ExecutorService executor;
    @Getter
    private Storage storage;
    @Getter
    private FormatUtils formatUtils;
    @Getter
    private ItemUtils itemUtils;
    @Getter
    private NamespacedKey amountKey;
    @Getter
    private NamespacedKey uuidKey;

    private PaperCommandManager commandManager;

    @Override
    public void onEnable() {
        instance = this;

        try {
            loadConfigurations();

            if (!validateInitialConfig()) {
                disableSelf();
                return;
            }

            initializeCore();
            initializeStorage();
            initializeFeatures();

        } catch (Exception e) {
            e.printStackTrace();
            disableSelf();
        }
    }

    @Override
    public void onDisable() {
        if (storage != null) {
            try {
                storage.bulkSave();
                storage.close();
            } catch (Exception e) {
                getLogger().severe("Error while shutting down storage: " + e.getMessage());
                e.printStackTrace();
            }
        }

        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }

        if (cacheMap != null) {
            cacheMap.clear();
        }

        instance = null;
        getLogger().info("🛑 SimpleEconomy disabled.");
    }

    private boolean validateInitialConfig() {
        SettingsConfig settings = SettingsConfig.getInstance();

        if ("mysql".equalsIgnoreCase(settings.storageSystem()) &&
                "CHANGEME".equalsIgnoreCase(settings.getDBPassword())) {
            getLogger().severe("==========================================");
            getLogger().severe("⚠️  First-time MySQL setup detected!");
            getLogger().severe("👉  Please configure a secure database password in config.yml");
            getLogger().severe("==========================================");
            return false;
        }

        return true;
    }

    private void disableSelf() {
        getServer().getPluginManager().disablePlugin(this);
    }

    private void initializeCore() {
        SettingsConfig settings = SettingsConfig.getInstance();

        executor = Executors.newFixedThreadPool(settings.getThreadPoolSize());
        cacheMap = Maps.newConcurrentMap();
        topMap = Maps.newConcurrentMap();
        formatUtils = new FormatUtils();
        itemUtils = new ItemUtils();

        amountKey = new NamespacedKey(this, getName() + "_voucheramount");
        uuidKey = new NamespacedKey(this, getName() + "_voucheruuid");

        new VaultHook();

        if (settings.checkForUpdates()) {
            new UpdateUtils().checkForUpdates();
        }
    }

    private void initializeStorage() {
        SettingsConfig settings = SettingsConfig.getInstance();
        String system = settings.storageSystem().toLowerCase();

        switch (system) {
            case "sqlite" -> {
                storage = new SQLiteStorage(this);
                ((SQLiteStorage) storage).init();
            }
            case "file" -> storage = new FileStorage(getDataFolder(), this);
            case "mysql" -> {
                var info = new DatabaseInfo(
                        settings.getDBHost(),
                        settings.getDBUsername(),
                        settings.getDBPassword(),
                        settings.getDBPort(),
                        settings.getDBName(),
                        settings.getDBMaxPool(),
                        settings.getDBPrefixTable()
                        );
                storage = new MySQLStorage(this, info);
            }
            default -> {
                getLogger().severe("Invalid storage system: '" + system + "'. Disabling plugin.");
                disableSelf();
            }
        }
    }

    private void initializeFeatures() {
        registerListeners();
        registerCommands();
        new AutoSaveTask(this).register();
        new BalTopRefreshTask(this).register();

        if (SettingsConfig.getInstance().registerPlaceholderAPI()) {
            if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
                getLogger().warning(
                        "PlaceholderAPI not detected, but 'use-placeholderapi' is enabled. Please install PlaceholderAPI or disable this option.");
            } else {
                new PAPIExpansion().register();
            }
        }
    }

    private void registerListeners() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerListener(this), this);

        if (SettingsConfig.getInstance().areVoucherEnabled()) {
            pm.registerEvents(new VoucherEvents(), this);
        }
    }

    private void registerCommands() {
        commandManager = new PaperCommandManager(this);

        commandManager.registerCommand(new SECommand());
        commandManager.registerCommand(new ECOCommand());
        commandManager.registerCommand(new BalanceCommand());
        commandManager.registerCommand(new PayCommand());
        commandManager.registerCommand(new BalTopCommand());

        if (SettingsConfig.getInstance().areVoucherEnabled()) {
            commandManager.registerCommand(new VoucherCommand());
        }
    }

    private void loadConfigurations() {
        SettingsConfig.getInstance().registerLightConfig(this);
        LangConfig.getInstance().registerConfig(this);
    }

    public void reloadConfigurations() {
        SettingsConfig.getInstance().reload();
        LangConfig.getInstance().reload();
    }
}
