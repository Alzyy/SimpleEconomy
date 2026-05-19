package it.alzy.simpleeconomy.plugin.tasks;

import org.bukkit.scheduler.BukkitRunnable;

import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.configurations.SettingsConfig;

public class BalTopRefreshTask extends BukkitRunnable {

    private final SimpleEconomy plugin;
    
    public BalTopRefreshTask(SimpleEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {

   
        plugin.getStorage().bulkSave();
    }

    public void register() {
        runTaskTimerAsynchronously(plugin, 0L, SettingsConfig.getInstance().getBalTopInterval() * 20L * 60L);
    }

}