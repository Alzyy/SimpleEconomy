package it.alzy.simpleeconomy.plugin.tasks;

import org.bukkit.scheduler.BukkitRunnable;

import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.configurations.SettingsConfig;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class BalTopRefreshTask extends BukkitRunnable {

    private final SimpleEconomy plugin;
    public BalTopRefreshTask(SimpleEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        ConcurrentMap<String, Double> topMap = new ConcurrentHashMap<>(plugin.getStorage().getTopBalances(
                SettingsConfig.getInstance().getTopPlayersShown()
        ));
        plugin.setTopMap(topMap);
    }


    public void register() {
        runTaskTimerAsynchronously(plugin, 0L, SettingsConfig.getInstance().getBalTopInterval() * 20L * 60L);
    }

}
