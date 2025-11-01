package it.alzy.simpleeconomy.plugin.tasks;

import org.bukkit.scheduler.BukkitRunnable;

import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.configurations.SettingsConfig;
import it.alzy.simpleeconomy.plugin.utils.UpdateUtils;

public class CheckUpdateTask extends BukkitRunnable {

    private final SimpleEconomy plugin;
    public CheckUpdateTask(SimpleEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        String latestVersion = UpdateUtils.getNewVersion();
        if (latestVersion == null) {
            plugin.getLogger().warning("Could not fetch the latest version information.");
            return;
        }

        String currentVersion = plugin.getDescription().getVersion();;
        if (!currentVersion.equals(latestVersion)) {
            plugin.getLogger().info("A new version of SimpleEconomy is available! (Current: " + currentVersion + ", Latest: " + latestVersion + ")");
            plugin.getLogger().info("Download it from: https://www.spigotmc.org/resources/simpleeconomy.127423/");
        } else {
            plugin.getLogger().info("You are using the latest version of SimpleEconomy (" + currentVersion + ").");
        }
    }


    public void register() {
        runTaskTimerAsynchronously(plugin, 0L, SettingsConfig.getInstance().getUpdateCheckInterval() * 20L * 60L);
    }

}
