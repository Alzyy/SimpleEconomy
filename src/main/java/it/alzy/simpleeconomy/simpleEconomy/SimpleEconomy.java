package it.alzy.simpleeconomy.simpleEconomy;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.collect.Maps;

import co.aikar.commands.PaperCommandManager;
import it.alzy.simpleeconomy.simpleEconomy.commands.BalanceCommand;
import it.alzy.simpleeconomy.simpleEconomy.commands.ECOCommand;
import it.alzy.simpleeconomy.simpleEconomy.commands.PayCommand;
import it.alzy.simpleeconomy.simpleEconomy.commands.SECommand;
import it.alzy.simpleeconomy.simpleEconomy.configurations.LangConfig;
import it.alzy.simpleeconomy.simpleEconomy.configurations.SettingsConfig;
import it.alzy.simpleeconomy.simpleEconomy.events.PlayerListener;
import it.alzy.simpleeconomy.simpleEconomy.storage.Storage;
import it.alzy.simpleeconomy.simpleEconomy.storage.impl.FileStorage;
import it.alzy.simpleeconomy.simpleEconomy.storage.impl.SQLiteStorage;
import it.alzy.simpleeconomy.simpleEconomy.utils.FormatUtils;
import it.alzy.simpleeconomy.simpleEconomy.utils.VaultHook;
import lombok.Getter;

public final class SimpleEconomy extends JavaPlugin {

    @Getter
    private static SimpleEconomy instance;

    @Getter
    private ConcurrentMap<UUID, Double> cacheMap;

    @Getter
    private ExecutorService executor;

    @Getter
    private Storage storage;

    @Getter
    private FormatUtils formatUtils;

    private PaperCommandManager commandManager;

    @Override
    public void onEnable() {
        instance = this;

        // Load configuration and thread pool
        loadConfigurations();
        executor = Executors.newFixedThreadPool(SettingsConfig.getInstance().getThreadPoolSize());

        // Initialize storage and events
        commandManager = new PaperCommandManager(this);
        loadStorageSystem();
        registerListeners();
        registerCommands();
        cacheMap = Maps.newConcurrentMap();

        // Initialize utils
        formatUtils = new FormatUtils();
        new VaultHook();
    }

    private void registerCommands() {
        commandManager.registerCommand(new SECommand());
        commandManager.registerCommand(new ECOCommand());
        commandManager.registerCommand(new BalanceCommand());
        commandManager.registerCommand(new PayCommand());

    }

    @Override
    public void onDisable() {
        // Persist cache to storage
        if (!cacheMap.isEmpty()) {
            if (storage instanceof FileStorage fileStorage) {
                fileStorage.bulkSaveAndShutdown();
            } else if (storage instanceof SQLiteStorage sqliteStorage) {
                sqliteStorage.bulkSaveAndShutdown();
            }
        }

        // Shutdown executor safely
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }

        // CLosing Hikari Pool
        if (storage instanceof SQLiteStorage sqliteStorage) {
            sqliteStorage.close();
        }

        instance = null;
    }

    private void loadStorageSystem() {
        String system = SettingsConfig.getInstance().storageSystem().toLowerCase();

        switch (system) {
            case "sqlite":
                storage = new SQLiteStorage(this);
                ((SQLiteStorage) storage).init();
                break;
            case "file":
                storage = new FileStorage(getDataFolder(), this);
                break;
            default:
                getLogger().severe("Invalid storage system: " + system + ". Disabling plugin.");
                getServer().getPluginManager().disablePlugin(this);
                break;

        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
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