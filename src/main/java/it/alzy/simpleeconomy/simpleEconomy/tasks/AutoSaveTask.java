package it.alzy.simpleeconomy.simpleEconomy.tasks;

import org.bukkit.scheduler.BukkitRunnable;

import it.alzy.simpleeconomy.simpleEconomy.SimpleEconomy;
import it.alzy.simpleeconomy.simpleEconomy.configurations.SettingsConfig;
import it.alzy.simpleeconomy.simpleEconomy.storage.Storage;
import it.alzy.simpleeconomy.simpleEconomy.storage.impl.FileStorage;
import it.alzy.simpleeconomy.simpleEconomy.storage.impl.SQLiteStorage;

public class AutoSaveTask extends BukkitRunnable {

    private final SimpleEconomy plugin;
    public AutoSaveTask(SimpleEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        Storage storage = plugin.getStorage();
        plugin.getLogger().info(String.format("Saving data for %d players from cache has started.", plugin.getCacheMap().size()));
        if (storage instanceof FileStorage fStorage) {
            fStorage.bulkSaveAndShutdown();
        } else if (storage instanceof SQLiteStorage sLiteStorage) {
            sLiteStorage.bulkSaveAndShutdown();
        }
    }


    public void register() {   
        runTaskTimerAsynchronously(plugin, 0L, SettingsConfig.getInstance().autoSaveInterval() * 20L * 60L);
    }
    
}
