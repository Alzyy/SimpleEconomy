package it.alzy.simpleeconomy.simpleEconomy.tasks;

import org.bukkit.scheduler.BukkitRunnable;

import it.alzy.simpleeconomy.simpleEconomy.SimpleEconomy;
import it.alzy.simpleeconomy.simpleEconomy.configurations.SettingsConfig;
import it.alzy.simpleeconomy.simpleEconomy.utils.UpdateUtils;

public class CheckUpdateTask extends BukkitRunnable {

    private final SimpleEconomy plugin;
    public CheckUpdateTask(SimpleEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        String latestVersion = UpdateUtils.getLatestVersion();
        if (latestVersion == null) {
            plugin.getLogger().warning("Could not fetch the latest version information.");
            return;
        }

        String currentVersion = plugin.getPluginMeta().getVersion();
        if (!currentVersion.equals(latestVersion)) {
            plugin.getLogger().info("A new version of SimpleEconomy is available! (Current: " + currentVersion + ", Latest: " + latestVersion + ")");
            plugin.getLogger().info("Download it from: https://www.spigotmc.org/resources/simpleeconomy.103182/");
        } else {
            plugin.getLogger().info("You are using the latest version of SimpleEconomy (" + currentVersion + ").");
        }
    }


    public void register() {
        runTaskTimerAsynchronously(plugin, 0L, SettingsConfig.getInstance().getBalTopInterval() * 20L * 60L);
    }

}
