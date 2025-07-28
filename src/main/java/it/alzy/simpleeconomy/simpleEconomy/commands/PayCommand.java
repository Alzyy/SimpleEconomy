package it.alzy.simpleeconomy.simpleEconomy.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import it.alzy.simpleeconomy.simpleEconomy.SimpleEconomy;
import it.alzy.simpleeconomy.simpleEconomy.configurations.LangConfig;
import it.alzy.simpleeconomy.simpleEconomy.utils.ChatUtils;
import it.alzy.simpleeconomy.simpleEconomy.utils.VaultHook;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@CommandAlias("pay")
@Description("Sends money to a player")
public class PayCommand extends BaseCommand {

    private final SimpleEconomy plugin = SimpleEconomy.getInstance();
    private final LangConfig config = LangConfig.getInstance();

    @Default
    @CommandCompletion("@players")
    public void root(Player sender, String targetName, double amount) {

        if (amount <= 0) {
            ChatUtils.send(sender, config.NEGATIVE_AMOUNT, "%prefix%", config.PREFIX);
            return;
        }

        if (sender.getName().equalsIgnoreCase(targetName)) {
            ChatUtils.send(sender, config.SELF, "%prefix%", config.PREFIX);
            return;
        }

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || target.getName() == null) {
            ChatUtils.send(sender, config.PLAYER_NOT_FOUND, "%prefix%", config.PREFIX);
            return;
        }

        plugin.getExecutor().execute(() -> {
            if (VaultHook.getEconomy() == null) {
                return;
            }

            double senderBalance = VaultHook.getEconomy().getBalance(sender);
            if (senderBalance < amount) {
                Bukkit.getScheduler().runTask(plugin, () -> ChatUtils.send(sender, config.NOT_ENOUGH_MONEY,
                        "%prefix%", config.PREFIX,
                        "%balance%", plugin.getFormatUtils().formatBalance(senderBalance)));
                return;
            }

            EconomyResponse withdrawal = VaultHook.getEconomy().withdrawPlayer(sender, amount);
            if (!withdrawal.transactionSuccess()) {
                return;
            }

            EconomyResponse deposit = VaultHook.getEconomy().depositPlayer(target, amount);
            if (!deposit.transactionSuccess()) {
                VaultHook.getEconomy().depositPlayer(sender, amount); 
                return;
            }

            double newSenderBalance = VaultHook.getEconomy().getBalance(sender);
            double newTargetBalance = VaultHook.getEconomy().getBalance(target);

            plugin.getCacheMap().put(sender.getUniqueId(), newSenderBalance);
            plugin.getCacheMap().put(target.getUniqueId(), newTargetBalance);
            plugin.getStorage().save(sender.getUniqueId(), newSenderBalance);
            plugin.getStorage().save(target.getUniqueId(), newTargetBalance);

            String formattedAmount = plugin.getFormatUtils().formatBalance(amount);
            String targetDisplayName = target.getName() != null ? target.getName() : "Unknown";

            Bukkit.getScheduler().runTask(plugin, () -> {
                ChatUtils.send(sender, config.GAVE_MONEY,
                        "%prefix%", config.PREFIX,
                        "%amount%", formattedAmount,
                        "%target%", targetDisplayName);

                if (target.isOnline()) {
                    Player targetPlayer = target.getPlayer();
                    if (targetPlayer != null) {
                        ChatUtils.send(targetPlayer, config.RECEIVED_MONEY,
                                "%prefix%", config.PREFIX,
                                "%amount%", formattedAmount,
                                "%source%", sender.getName());
                    }
                }
            });
        });
    }

}
