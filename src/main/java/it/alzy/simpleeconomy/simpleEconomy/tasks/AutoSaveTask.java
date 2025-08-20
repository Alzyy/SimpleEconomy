package it.alzy.simpleeconomy.simpleEconomy.tasks;

import org.bukkit.scheduler.BukkitRunnable;

import it.alzy.simpleeconomy.simpleEconomy.SimpleEconomy;
import it.alzy.simpleeconomy.simpleEconomy.configurations.SettingsConfig;
import it.alzy.simpleeconomy.simpleEconomy.storage.Storage;

public class AutoSaveTask extends BukkitRunnable {

    private final SimpleEconomy plugin;
    public AutoSaveTask(SimpleEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if(plugin.getCacheMap().isEmpty()) return;
        Storage storage = plugin.getStorage();
        plugin.getLogger().info(String.format("Saving data for %d players from cache has started.", plugin.getCacheMap().size()));
        storage.bulkSave();
    }


    public void register() {   
        runTaskTimerAsynchronously(plugin, 0L, SettingsConfig.getInstance().autoSaveInterval() * 20L * 60L);
    }
    
}
