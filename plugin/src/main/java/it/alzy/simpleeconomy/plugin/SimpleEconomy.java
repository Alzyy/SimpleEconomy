package it.alzy.simpleeconomy.plugin;

import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.PaperCommandManager;
import it.alzy.simpleeconomy.api.SimpleEconomyAPI;
import it.alzy.simpleeconomy.plugin.api.PAPIExpansion;
import it.alzy.simpleeconomy.plugin.api.internal.EconomyProviderImpl;
import it.alzy.simpleeconomy.plugin.commands.*;
import it.alzy.simpleeconomy.plugin.configurations.CurrenciesConfig;
import it.alzy.simpleeconomy.plugin.configurations.SettingsConfig;
import it.alzy.simpleeconomy.plugin.events.PlayerListener;
import it.alzy.simpleeconomy.plugin.events.VoucherEvents;
import it.alzy.simpleeconomy.plugin.i18n.LanguageManager;
import it.alzy.simpleeconomy.plugin.i18n.enums.LanguageKeys;
import it.alzy.simpleeconomy.plugin.logging.TransactionLogger;
import it.alzy.simpleeconomy.plugin.logging.WebhookLogger;
import it.alzy.simpleeconomy.plugin.managers.CurrencyManager;
import it.alzy.simpleeconomy.plugin.managers.ModuleManager;
import it.alzy.simpleeconomy.plugin.model.VirtualCurrency;
import it.alzy.simpleeconomy.plugin.records.DatabaseInfo;
import it.alzy.simpleeconomy.plugin.storage.Cache;
import it.alzy.simpleeconomy.plugin.storage.Storage;
import it.alzy.simpleeconomy.plugin.storage.impl.FileStorage;
import it.alzy.simpleeconomy.plugin.storage.impl.MySQLStorage;
import it.alzy.simpleeconomy.plugin.storage.impl.SQLiteStorage;
import it.alzy.simpleeconomy.plugin.tasks.*;
import it.alzy.simpleeconomy.plugin.utils.FormatUtils;
import it.alzy.simpleeconomy.plugin.utils.ItemUtils;
import it.alzy.simpleeconomy.plugin.utils.TransactionHelper;
import it.alzy.simpleeconomy.plugin.utils.UpdateUtils;
import it.alzy.simpleeconomy.plugin.utils.VaultHook;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimpleEconomy extends JavaPlugin {

    @Getter
    private static SimpleEconomy instance;

    @Getter
    private final Cache cache = new Cache();

    @Getter @Setter
    private Map<String, Double> topMap;
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
    private CurrencyManager currencyManager;

    @Getter
    private UpdateUtils updateUtils;
    @Getter
    private LanguageManager languageManager;

    @Getter
    private TransactionHelper transactionHelper;

    @Getter private TransactionLogger transactionLogger;

    @Getter private WebhookLogger webhookLogger;

    @Getter
    private ModuleManager moduleManager;
    @Getter
    PaperCommandManager commandManager;

    @Getter
    private boolean isPaper;

    @Override
    public void onEnable() {
        instance = this;
        try {
            Class.forName("it.alzy.simpleeconomy.plugin.utils.ChatUtils");
        } catch(ClassNotFoundException e) {
            // ignored
        }

        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            isPaper = true;
        } catch (ClassNotFoundException e) {
            getLogger().info("Paper API not detected, running in Spigot/Bukkit mode.");
            isPaper = false;
        }

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            getLogger().severe("Uncaught exception in thread " + thread.getName() + ": " + throwable.getMessage());
            throwable.printStackTrace();
        });

        try {
            loadConfigurations();

            if (!validateInitialConfig()) {
                disableSelf();
                return;
            }

            initializeCore();
            initializeStorage();
            initializeFeatures();
            moduleManager = new ModuleManager(this);
            moduleManager.loadModules();
            
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

        if (cache != null) {
            cache.invalidateAll();
        }

        if(topMap != null) {
            topMap.clear();
        }
        if(languageManager != null) {
            languageManager.unloadLanguages();
        }

        if(transactionLogger != null) {
            transactionLogger.close();
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
            try {
                this.bukkitAudiences = BukkitAudiences.create(this);
            } catch (Throwable t) {
                getLogger().warning("Adventure audiences unavailable; falling back to Bukkit messages.");
                isPaper = true;
            }
        }
        executor = Executors.newFixedThreadPool(settings.getThreadPoolSize());
        topMap = new LinkedHashMap<>();
        languageManager = new LanguageManager(this, SettingsConfig.getInstance().locale());
        transactionHelper = new TransactionHelper(this, languageManager);

        formatUtils = new FormatUtils();
        itemUtils = new ItemUtils();
        amountKey = new NamespacedKey(this, getName() + "_voucheramount");
        uuidKey = new NamespacedKey(this, getName() + "_voucheruuid");

        if (isClassPresent("net.milkbowl.vault.economy.Economy")) {
            new VaultHook();
        } else {
            getLogger().info("Vault API not detected; skipping Vault hook setup.");
        }

        if (settings.checkForUpdates()) {
            updateUtils = new UpdateUtils();
            updateUtils.checkForUpdates();
            new CheckUpdateTask(this).register();
        }
    }

    private boolean isClassPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
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
        if(SettingsConfig.getInstance().isInterestEnabled()) {
            new InterestTask(this).register();
        }
        if (SettingsConfig.getInstance().registerPlaceholderAPI()) {
            if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
                getLogger().warning(
                        "PlaceholderAPI not detected, but 'use-placeholderapi' is enabled. Please install PlaceholderAPI or disable this option.");
            } else {
                new PAPIExpansion().register();
            }
        }

        if(SettingsConfig.getInstance().isTransactionLoggingEnabled()) {
            transactionLogger = new TransactionLogger(this);
            transactionLogger.init();
        }

        if(SettingsConfig.getInstance().shouldLogToDiscord()) {
            webhookLogger = new WebhookLogger();
        }

        if(SettingsConfig.getInstance().isAutoPurgeEnabled()) {
            new AutoPurgeTask(this).register();
        }
        loadApis();
        currencyManager = new CurrencyManager();
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
        commandManager = new PaperCommandManager(this);
        commandManager.getCommandContexts().registerContext(Double.class, c -> {
            String arg = c.popFirstArg();

            try {
                double val = Double.parseDouble(arg);
                if (!Double.isFinite(val) || val < 0) {
                    throw new NumberFormatException();
                }
                return Math.floor(val * 100) / 100;
            } catch (NumberFormatException e) {
                languageManager.send(c.getSender(), LanguageKeys.INVALID_AMOUNT, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX), "%arg%", arg);
                throw new InvalidCommandArgument(false);
            }
        });
        commandManager.getCommandCompletions().registerAsyncCompletion("currencies", c -> {
            return getCurrencyManager().getAllCurrencies().stream()
                    .map(VirtualCurrency::getName)
                    .toList();
        });

        commandManager.getCommandCompletions().registerAsyncCompletion("modules", c -> {
            return moduleManager.getLoadedModuleNames().stream().toList();
        });

        commandManager.getCommandCompletions().registerAsyncCompletion("modulesFiles", c -> {
            File modulesDir = new File(getDataFolder(), "modules");
            if (!modulesDir.exists() || !modulesDir.isDirectory()) {
                return List.of();
            }
            return Arrays.stream(modulesDir.listFiles((dir, name) -> name.endsWith(".jar")))
                    .map(File::getName)
                    .toList();
        });
        

        commandManager.registerCommand(new SECommand());
        commandManager.registerCommand(new ECOCommand());
        commandManager.registerCommand(new BalanceCommand());
        commandManager.registerCommand(new PayCommand());
        commandManager.registerCommand(new BalTopCommand());
        commandManager.registerCommand(new CurrenciesCommand());
        commandManager.registerCommand(new ModulesCommand());

        if (SettingsConfig.getInstance().areVoucherEnabled()) {
            commandManager.registerCommand(new VoucherCommand());
        }

        if(SettingsConfig.getInstance().isTransactionLoggingEnabled()) {
            commandManager.registerCommand(new ECOHistoryCommand());
        }
    }

    public void runAsync(Runnable task) {
        if(executor == null || executor.isShutdown()) {
            getLogger().severe("Executor service is not available. Cannot run async task.");
            return;
        }
        executor.execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                getLogger().severe("An error occurred while executing an asynchronous task: " + e.getMessage());
            }
        });
    }
    private void loadConfigurations() {
        SettingsConfig.getInstance().registerLightConfig(this);
        SettingsConfig.getInstance().checkMissingKeys();
        CurrenciesConfig.getInstance().registerLightConfig(this);
    }

}
