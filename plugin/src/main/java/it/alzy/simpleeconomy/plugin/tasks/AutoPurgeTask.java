package it.alzy.simpleeconomy.plugin.tasks;

import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.configurations.SettingsConfig;
import org.bukkit.scheduler.BukkitRunnable;

public class AutoPurgeTask extends BukkitRunnable {

    private final SimpleEconomy plugin;
    public AutoPurgeTask(SimpleEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        plugin.getStorage().purge(SettingsConfig.getInstance().getPurgeInactiveAccountsDays());
    }


    public void register() {
        runTaskTimerAsynchronously(plugin, 0L, 1600L * 60L * 60L * 24L); // 24 hours
    }

}