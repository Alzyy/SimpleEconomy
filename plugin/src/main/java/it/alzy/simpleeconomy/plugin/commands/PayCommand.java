package it.alzy.simpleeconomy.plugin.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.i18n.LanguageManager;
import it.alzy.simpleeconomy.plugin.i18n.enums.LanguageKeys;
import it.alzy.simpleeconomy.plugin.utils.VaultHook;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.math.BigDecimal;

@CommandAlias("pay")
@Description("Sends money to a player")
public class PayCommand extends BaseCommand {

    private final SimpleEconomy plugin = SimpleEconomy.getInstance();
    private final LanguageManager languageManager = plugin.getLanguageManager();

    @Default
    @CommandCompletion("@players")
    public void root(Player sender, @Optional String targetName, @Optional Double amount) {
        if (targetName == null || targetName.isEmpty() || amount == null || amount == 0) {
            languageManager.send(sender, LanguageKeys.PAY_USAGE, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
            return;
        }

        if (!Double.isFinite(amount) || amount <= 0 || BigDecimal.valueOf(amount).scale() > 2) {
            languageManager.send(sender, LanguageKeys.INVALID_AMOUNT, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
            return;
        }

        if (sender.getName().equalsIgnoreCase(targetName)) {
            languageManager.send(sender, LanguageKeys.SELF_COMMAND, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
            return;
        }

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            languageManager.send(sender, LanguageKeys.PLAYER_NOT_FOUND, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
            return;
        }

        Economy economy = VaultHook.getEconomy();
        if (economy == null) {
            return;
        }

        plugin.getExecutor().execute(() -> {
            double senderBalance = economy.getBalance(sender);
            if (senderBalance < amount) {
                languageManager.send(sender, LanguageKeys.NOT_ENOUGH_MONEY,
                        "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX),
                        "%balance%", plugin.getFormatUtils().formatBalance(senderBalance));
                return;
            }

            EconomyResponse withdrawal = economy.withdrawPlayer(sender, amount);
            if (!withdrawal.transactionSuccess())
                return;

            EconomyResponse deposit = economy.depositPlayer(target, amount);
            if (!deposit.transactionSuccess()) {
                economy.depositPlayer(sender, amount); 
                return;
            }

            double newSenderBalance = economy.getBalance(sender);
            double newTargetBalance = economy.getBalance(target);

            plugin.getCacheMap().put(sender.getUniqueId(), newSenderBalance);
            plugin.getCacheMap().put(target.getUniqueId(), newTargetBalance);

            plugin.getStorage().save(sender.getUniqueId(), newSenderBalance);
            plugin.getStorage().save(target.getUniqueId(), newTargetBalance);

            String formattedAmount = plugin.getFormatUtils().formatBalance(amount);

            Bukkit.getScheduler().runTask(plugin, () -> {
                languageManager.send(sender, LanguageKeys.GAVE_MONEY,
                        "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX),
                        "%amount%", formattedAmount,
                        "%target%", target.getName());

                if (target.isOnline()) {
                    languageManager.send(sender, LanguageKeys.RECEIVED_MONEY,
                            "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX),
                            "%amount%", formattedAmount,
                            "%source%", sender.getName());
                }
            });
        });
    }
}
