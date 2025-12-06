package it.alzy.simpleeconomy.plugin;

import co.aikar.commands.PaperCommandManager;
import com.google.common.collect.Maps;

import it.alzy.simpleeconomy.api.SimpleEconomyAPI;
import it.alzy.simpleeconomy.plugin.api.PAPIExpansion;
import it.alzy.simpleeconomy.plugin.api.internal.EconomyProviderImpl;
import it.alzy.simpleeconomy.plugin.commands.*;
import it.alzy.simpleeconomy.plugin.configurations.LangConfig;
import it.alzy.simpleeconomy.plugin.configurations.SettingsConfig;
import it.alzy.simpleeconomy.plugin.events.PlayerListener;
import it.alzy.simpleeconomy.plugin.events.VoucherEvents;
import it.alzy.simpleeconomy.plugin.records.DatabaseInfo;
import it.alzy.simpleeconomy.plugin.storage.Storage;
import it.alzy.simpleeconomy.plugin.storage.impl.FileStorage;
import it.alzy.simpleeconomy.plugin.storage.impl.MySQLStorage;
import it.alzy.simpleeconomy.plugin.storage.impl.SQLiteStorage;
import it.alzy.simpleeconomy.plugin.tasks.AutoSaveTask;
import it.alzy.simpleeconomy.plugin.tasks.BalTopRefreshTask;
import it.alzy.simpleeconomy.plugin.tasks.CheckUpdateTask;
import it.alzy.simpleeconomy.plugin.utils.*;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
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

    @Getter
    private BukkitAudiences bukkitAudiences;

    @Getter
    private UpdateUtils updateUtils;

    @Getter
    private boolean isPaper;

    @Override
    public void onEnable() {
        instance = this;
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            isPaper = true;
        } catch (ClassNotFoundException e) {
            getLogger().info("Paper API not detected, running in Spigot/Bukkit mode.");
            isPaper = false;
        }

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
            getLogger().severe("An error occurred during plugin initialization: " + e.getMessage());
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
        if(!isPaper) {
            this.bukkitAudiences = BukkitAudiences.create(this);
        }
        executor = Executors.newFixedThreadPool(settings.getThreadPoolSize());
        cacheMap = Maps.newConcurrentMap();
        topMap = Maps.newConcurrentMap();
        formatUtils = new FormatUtils();
        itemUtils = new ItemUtils();

        amountKey = new NamespacedKey(this, getName() + "_voucheramount");
        uuidKey = new NamespacedKey(this, getName() + "_voucheruuid");

        new VaultHook();

        if (settings.checkForUpdates()) {
            updateUtils = new UpdateUtils();
            updateUtils.checkForUpdates();
            new CheckUpdateTask(this).register();
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
        loadApis();
    }

    private void loadApis() {
        EconomyProviderImpl economyProvider = new EconomyProviderImpl(this);
        SimpleEconomyAPI.setProvider(economyProvider);
        getLogger().info("SimpleEconomy's external API loaded successfully.");
    }

    private void registerListeners() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerListener(), this);

        if (SettingsConfig.getInstance().areVoucherEnabled()) {
            pm.registerEvents(new VoucherEvents(), this);
        }
    }

    private void registerCommands() {
        PaperCommandManager commandManager = new PaperCommandManager(this);

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
