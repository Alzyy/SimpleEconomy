package it.alzy.simpleeconomy.simpleEconomy.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import it.alzy.simpleeconomy.simpleEconomy.SimpleEconomy;
import it.alzy.simpleeconomy.simpleEconomy.configurations.LangConfig;
import it.alzy.simpleeconomy.simpleEconomy.utils.ChatUtils;

@CommandAlias("balance|bal|money")
@Description("Displays the balance of a player. Shows your own balance if no player is specified. " +
        "Viewing others' balances requires permission.")
public class BalanceCommand extends BaseCommand {

    private final SimpleEconomy plugin = SimpleEconomy.getInstance();
    private final LangConfig config = LangConfig.getInstance();

    @Default
    public void onBalance(Player player, @Optional String targetName) {
        if (targetName == null || targetName.isEmpty()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                double balance = plugin.getCacheMap().getOrDefault(player.getUniqueId(), 0d);
                String formattedBalance = plugin.getFormatUtils().formatBalance(balance);
                ChatUtils.send(player, config.BALANCE_CHECK_SELF,
                        "%prefix%", config.PREFIX,
                        "%balance%", formattedBalance);
            });
        } else {
            if (!player.hasPermission("simpleconomy.balance.others")) {
                ChatUtils.send(player, config.NO_PERMISSION, "%prefix%", config.PREFIX);
                return;
            }

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
                if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
                    ChatUtils.send(player, config.PLAYER_NOT_FOUND, "%prefix%", config.PREFIX);
                    return;
                }

                double balance = plugin.getCacheMap().getOrDefault(target.getUniqueId(), 0d);
                String formattedBalance = plugin.getFormatUtils().formatBalance(balance);
                ChatUtils.send(player, config.BALANCE_CHECK_OTHER,
                        "%prefix%", config.PREFIX,
                        "%balance%", formattedBalance,
                        "%target%", targetName);
            });
        }
    }
}
