package it.alzy.simpleeconomy.plugin.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.i18n.LanguageManager;
import it.alzy.simpleeconomy.plugin.i18n.enums.LanguageKeys;
import it.alzy.simpleeconomy.plugin.utils.TransactionHelper;
import it.alzy.simpleeconomy.plugin.utils.VaultHook;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import co.aikar.commands.annotation.Optional;


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

        TransactionHelper helper = plugin.getTransactionHelper();

        if (!helper.validateAmount(sender, amount)) {
            return;
        }

        if (!helper.checkTransactionLimit(sender, amount)) {
            return;
        }

        if (sender.getName().equalsIgnoreCase(targetName)) {
            languageManager.send(sender, LanguageKeys.SELF_COMMAND, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
            return;
        }

        java.util.Optional<Player> targetOptional = helper.resolveOnlineTarget(sender, targetName);
        if (targetOptional.isEmpty()) {
            return;
        }
        Player target = targetOptional.get();

        Economy economy = VaultHook.getEconomy();
        if (economy == null) {
            return;
        }

        if (!economy.has(sender, amount)) {
            languageManager.send(sender, LanguageKeys.NOT_ENOUGH_MONEY, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX), "%balance%", plugin.getFormatUtils().formatBalance(economy.getBalance(sender)));
            return;
        }

        EconomyResponse withdrawal = economy.withdrawPlayer(sender, amount);
        if (!withdrawal.transactionSuccess()) {
            return;
        }

        EconomyResponse deposit = economy.depositPlayer(target, amount);
        if (!deposit.transactionSuccess()) {
            economy.depositPlayer(sender, amount);
            return;
        }

        String formattedAmount = plugin.getFormatUtils().formatBalance(amount);
        languageManager.send(sender, LanguageKeys.GAVE_MONEY, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX), "%amount%", formattedAmount, "%target%", target.getName());
        languageManager.send(target, LanguageKeys.RECEIVED_MONEY, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX), "%amount%", formattedAmount, "%source%", sender.getName());

        helper.commitTransferAsync(
                sender.getUniqueId(),
                target.getUniqueId(),
                "money",
                amount,
                withdrawal.balance,
                deposit.balance,
                sender.getName(),
                target.getName()
        );
    }
}