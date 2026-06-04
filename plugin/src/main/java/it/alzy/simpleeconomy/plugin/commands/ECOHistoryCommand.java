package it.alzy.simpleeconomy.plugin.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.i18n.LanguageManager;
import it.alzy.simpleeconomy.plugin.i18n.enums.LanguageKeys;
import it.alzy.simpleeconomy.plugin.records.Transaction;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.UUID;

import static it.alzy.simpleeconomy.plugin.utils.TimeUtils.dateTime;

@CommandAlias("ehistory")
@Description("Displays the transaction history of a player.")
public class ECOHistoryCommand extends BaseCommand {

    private final SimpleEconomy plugin;
    private final LanguageManager languageManager;

    public ECOHistoryCommand() {
        this.plugin = SimpleEconomy.getInstance();
        this.languageManager = plugin.getLanguageManager();
    }

    @Default
    @Syntax("<player> [limit] [currency]")
    @CommandCompletion("@players @range:1-20 @currencies")
    public void history(CommandSender sender, @Optional String targetName, @Optional @Default("10") Integer limit, @Optional @Default("money") String currency) {
        if (!sender.hasPermission("simpleconomy.eco.history")) {
            languageManager.send(sender, LanguageKeys.NO_PERMISSION, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
            return;
        }

        if (targetName == null || targetName.isEmpty() || limit == null || limit <= 0) {
            languageManager.send(sender, LanguageKeys.ECO_HISTORY_USAGE, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
            return;
        }

        String prefix = languageManager.getMessage(LanguageKeys.PREFIX);
        String targetCurrency = currency.toLowerCase();

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            languageManager.send(sender, LanguageKeys.PLAYER_NOT_FOUND, "%prefix%", prefix);
            return;
        }

        String resolvedName = target.getName() != null ? target.getName() : targetName;
        languageManager.send(sender, LanguageKeys.ECO_HISTORY_FETCHING, "%prefix%", prefix, "%player%", resolvedName);

        plugin.getTransactionLogger().getHistoryOfPlayer(target.getUniqueId().toString(), targetCurrency, limit).thenAccept(transactions -> {
            if (transactions == null || transactions.isEmpty()) {
                languageManager.send(sender, LanguageKeys.ECO_HISTORY_NO_ENTRIES, "%prefix%", prefix, "%player%", resolvedName);
                return;
            }

            languageManager.send(sender, LanguageKeys.ECO_HISTORY_HEADER, "%prefix%", prefix, "%player%", resolvedName);

            var formatUtils = plugin.getFormatUtils();
            var currencyInfo = plugin.getCurrencyManager().getCurrency(targetCurrency);

            for (Transaction transaction : transactions) {
                String targetUUIDString = transaction.targetUUID();
                String targetDisplayName = "Server";

                if (targetUUIDString != null && !targetUUIDString.isEmpty()) {
                    try {
                        OfflinePlayer transTarget = Bukkit.getOfflinePlayer(UUID.fromString(targetUUIDString));
                        targetDisplayName = (transTarget.getName() != null) ? transTarget.getName() : "Unknown";
                    } catch (IllegalArgumentException ignored) {
                    }
                }

                String formattedAmount;
                if ("money".equals(targetCurrency)) {
                    formattedAmount = formatUtils.formatBalance(transaction.amount());
                } else if (currencyInfo != null) {
                    formattedAmount = formatUtils.formatVirtualCurrencyBalance(currencyInfo, transaction.amount());
                } else {
                    formattedAmount = String.valueOf(transaction.amount());
                }

                languageManager.send(sender, LanguageKeys.ECO_HISTORY_ENTRY,
                        "%type%", transaction.type().name(),
                        "%amount%", formattedAmount,
                        "%currency%", targetCurrency,
                        "%balance_before%", String.valueOf(transaction.balanceBefore()),
                        "%balance_after%", String.valueOf(transaction.balanceAfter()),
                        "%target%", targetDisplayName,
                        "%date%", dateTime(transaction.timestamp())
                );
            }
        });
    }
}