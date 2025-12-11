package it.alzy.simpleeconomy.plugin.commands;

import it.alzy.simpleeconomy.plugin.i18n.LanguageManager;
import it.alzy.simpleeconomy.plugin.i18n.enums.LanguageKeys;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import it.alzy.simpleeconomy.plugin.SimpleEconomy;

@CommandAlias("balance|bal|money")
@Description("Displays the balance of a player. Shows your own balance if no player is specified. Viewing others' balances requires permission.")
public class BalanceCommand extends BaseCommand {

    private final SimpleEconomy plugin = SimpleEconomy.getInstance();
    private final LanguageManager languageManager = plugin.getLanguageManager();
    @Default
    public void onBalance(Player player, @Optional String targetName) {
        if (targetName == null || targetName.isEmpty()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                double balance = plugin.getCacheMap().getOrDefault(player.getUniqueId(), 0d);
                String formattedBalance = plugin.getFormatUtils().formatBalance(balance);
                languageManager.send(player, LanguageKeys.BALANCE_CHECK_SELF, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX), "%balance%", formattedBalance);
            });
        } else {
            if (!player.hasPermission("simpleconomy.balance.others")) {
                languageManager.send(player, LanguageKeys.NO_PERMISSION, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
                return;
            }

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
                if (!target.hasPlayedBefore()) {
                    languageManager.send(player, LanguageKeys.PLAYER_NOT_FOUND, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX), "%target%", targetName);
                    return;
                }

                plugin.getStorage().getBalance(target.getUniqueId()).thenAccept(dbBalance ->  {
                    String formattedBalance = plugin.getFormatUtils().formatBalance(dbBalance);
                    languageManager.send(player, LanguageKeys.BALANCE_CHECK_OTHER, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX), "%balance%", formattedBalance, "%target%", targetName);
                });
            });
        }
    }
}
