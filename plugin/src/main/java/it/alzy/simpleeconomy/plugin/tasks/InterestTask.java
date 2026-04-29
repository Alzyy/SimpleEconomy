package it.alzy.simpleeconomy.plugin.tasks;

import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.configurations.SettingsConfig;
import it.alzy.simpleeconomy.plugin.i18n.enums.LanguageKeys;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.atomic.LongAdder;

public class InterestTask extends BukkitRunnable {
    private final SimpleEconomy plugin;

    public InterestTask(SimpleEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        SettingsConfig settings = SettingsConfig.getInstance();
        if (!settings.isInterestEnabled()) return;

        double rate = settings.getInterestRate();
        double minBalance = settings.minBalanceForInterests();
        double maxInterest = settings.maxInterest();

        LongAdder processedPlayers = new LongAdder();

        plugin.getLogger().info("Processing interest payout for online players...");

        Bukkit.getOnlinePlayers().parallelStream().forEach(p -> {
            plugin.getCacheMap().computeIfPresent(p.getUniqueId(), (uuid, current) -> {
                
                if (current < minBalance) return current;

                double bonus = Math.min(current * rate, maxInterest);

                if (bonus <= 0) return current;

                double newBalance = current + bonus;

                plugin.getLanguageManager().send(p, LanguageKeys.INTERESTS_RECEIVED,
                        "%prefix%", plugin.getLanguageManager().getMessage(LanguageKeys.PREFIX),
                        "%amount%", plugin.getFormatUtils().formatBalance(bonus));

                processedPlayers.increment();
                return newBalance;
            });
        });

        int total = processedPlayers.intValue();
        if (total > 0) {
            plugin.getLogger().info("Interest cycle finished. Payouts sent to " + total + " players.");
        } else {
            plugin.getLogger().info("Interest cycle finished. No eligible players found.");
        }
    }

    public void register() {
        runTaskTimerAsynchronously(plugin, 20L * 60L, SettingsConfig.getInstance().getInterestInterval() * 20L * 60L);
    }
}