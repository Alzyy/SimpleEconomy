package it.alzy.simpleeconomy.plugin.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import it.alzy.simpleeconomy.api.TransactionTypes;
import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.configurations.SettingsConfig;
import it.alzy.simpleeconomy.plugin.i18n.LanguageManager;
import it.alzy.simpleeconomy.plugin.i18n.enums.LanguageKeys;
import it.alzy.simpleeconomy.plugin.records.Transaction;
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
        if (limited(amount)) {
            double max = SettingsConfig.getInstance().getMaxTransactionLimit();
            languageManager.send(sender, LanguageKeys.AMOUNT_EXCEEDS_LIMIT,
                    "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX),
                    "%max%", plugin.getFormatUtils().formatBalance(max));
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

        double senderBalanceAfter = withdrawal.balance;
        double targetBalanceAfter = deposit.balance;
        String formattedAmount = plugin.getFormatUtils().formatBalance(amount);

        languageManager.send(sender, LanguageKeys.GAVE_MONEY, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX), "%amount%", formattedAmount, "%target%", target.getName());

        languageManager.send(target, LanguageKeys.RECEIVED_MONEY, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX), "%amount%", formattedAmount, "%source%", sender.getName());

        plugin.getExecutor().execute(() -> {
            plugin.getCacheMap().put(sender.getUniqueId(), senderBalanceAfter);
            plugin.getCacheMap().put(target.getUniqueId(), targetBalanceAfter);

            plugin.getStorage().save(sender.getUniqueId(), senderBalanceAfter);
            plugin.getStorage().save(target.getUniqueId(), targetBalanceAfter);

            if (SettingsConfig.getInstance().isTransactionLoggingEnabled()) {
                Transaction transaction = new Transaction(sender.getUniqueId().toString(), target.getUniqueId().toString(), amount, senderBalanceAfter + amount, senderBalanceAfter, TransactionTypes.PAY, System.currentTimeMillis());
                plugin.getTransactionLogger().appendLog(transaction);
            }
        });
    }

    public boolean limited(double amount) {
        double max = SettingsConfig.getInstance().getMaxTransactionLimit();
        if( max == 0) return false;
        return amount > max;
    }
}