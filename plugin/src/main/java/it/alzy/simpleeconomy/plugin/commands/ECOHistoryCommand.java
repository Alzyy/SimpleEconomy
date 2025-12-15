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

import java.util.AbstractMap;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
    @Syntax("<player> [limit]")
    @CommandCompletion("@players @range:1-20")
    public void history(CommandSender sender, @Optional String targetName, @Optional @Default("10") Integer limit) {
        if (!sender.hasPermission("simpleconomy.eco.history")) {
            languageManager.send(sender, LanguageKeys.NO_PERMISSION, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
            return;
        }

        if(targetName == null || targetName.isEmpty() || limit == null || limit <= 0) {
            languageManager.send(sender, LanguageKeys.ECO_HISTORY_USAGE, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
            return;
        }

        String prefix = languageManager.getMessage(LanguageKeys.PREFIX);
        languageManager.send(sender, LanguageKeys.ECO_HISTORY_FETCHING, "%prefix%", prefix, "%player%", targetName);

        CompletableFuture.supplyAsync(() -> {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            return (target.hasPlayedBefore() || target.isOnline()) ? target : null;
        }).thenCompose(target -> {
            if (target == null) return CompletableFuture.completedFuture(null);
            return plugin.getTransactionLogger().getHistoryOfPlayer(target.getUniqueId().toString(), limit)
                    .thenApply(history -> new AbstractMap.SimpleEntry<>(target, history));
        }).thenAccept(result -> {
            if (result == null) {
                languageManager.send(sender, LanguageKeys.PLAYER_NOT_FOUND, "%prefix%", prefix);
                return;
            }

            OfflinePlayer target = result.getKey();
            LinkedList<Transaction> transactions = result.getValue();

            if (transactions.isEmpty()) {
                languageManager.send(sender, LanguageKeys.ECO_HISTORY_NO_ENTRIES, "%prefix%", prefix, "%player%", target.getName());
                return;
            }

            languageManager.send(sender, LanguageKeys.ECO_HISTORY_HEADER, "%prefix%", prefix, "%player%", target.getName());

            var formatUtils = plugin.getFormatUtils();

            for (Transaction transaction : transactions) {
                String targetUUIDString = transaction.targetUUID();
                String targetDisplayName = "Server";

                if (targetUUIDString != null && !targetUUIDString.isEmpty()) {
                    try {
                        OfflinePlayer transTarget = Bukkit.getOfflinePlayer(UUID.fromString(targetUUIDString));
                        targetDisplayName = (transTarget.getName() != null) ? transTarget.getName() : "Unknown";
                    } catch (IllegalArgumentException ignored) {}
                }

                languageManager.send(sender, LanguageKeys.ECO_HISTORY_ENTRY,
                        "%type%", transaction.type().name(),
                        "%amount%", formatUtils.formatBalance(transaction.amount()),
                        "%balance_before%", transaction.balanceBefore(),
                        "%balance_after%", transaction.balanceAfter(),
                        "%target%", targetDisplayName,
                        "%date%", dateTime(transaction.timestamp())
                );
            }
        });
    }
}
