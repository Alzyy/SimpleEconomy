package it.alzy.simpleeconomy.plugin.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import it.alzy.simpleeconomy.api.EconomyProvider;
import it.alzy.simpleeconomy.api.SimpleEconomyAPI;
import it.alzy.simpleeconomy.api.TransactionTypes;
import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.i18n.LanguageManager;
import it.alzy.simpleeconomy.plugin.i18n.enums.LanguageKeys;
import it.alzy.simpleeconomy.plugin.utils.ChatUtils;
import it.alzy.simpleeconomy.plugin.utils.TransactionHelper;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@CommandAlias("eco")
@Description("Manage players' economy balances.")
public class ECOCommand extends BaseCommand {

    private final SimpleEconomy plugin = SimpleEconomy.getInstance();
    private final LanguageManager languageManager = plugin.getLanguageManager();

    @Default
    public void root(CommandSender player) {
        sendUsage(player);
    }

    @Subcommand("set")
    @CommandCompletion("@players|@a|@p|@r")
    @CommandPermission("simpleconomy.eco.set")
    public void set(CommandSender sender, @Optional String target, @Optional Double amount) {
        if (isInvalidInput(sender, target, amount)) return;
        execute(sender, target, amount, EcoAction.SET);
    }

    @Subcommand("give")
    @CommandCompletion("@players|@a|@p|@r")
    @CommandPermission("simpleconomy.eco.give")
    public void give(CommandSender sender, @Optional String target, @Optional Double amount) {
        if (isInvalidInput(sender, target, amount)) return;
        execute(sender, target, amount, EcoAction.GIVE);
    }

    @Subcommand("remove")
    @CommandCompletion("@players|@a|@p|@r")
    @CommandPermission("simpleconomy.eco.remove")
    public void remove(CommandSender sender, @Optional String target, @Optional Double amount) {
        if (isInvalidInput(sender, target, amount)) return;
        execute(sender, target, amount, EcoAction.REMOVE);
    }

    private void execute(CommandSender sender, String targetName, double amount, EcoAction action) {
        TransactionHelper helper = plugin.getTransactionHelper();

        boolean allowZero = (action == EcoAction.SET);
        if (helper.isNotVallidAmount(sender, amount, allowZero)) return;

        Collection<OfflinePlayer> targets = helper.resolveTargets(sender, targetName);
        if (targets.isEmpty()) return;

        String prefix = languageManager.getMessage(LanguageKeys.PREFIX);
        String currency = "money";

        final EconomyProvider provider = SimpleEconomyAPI.getProvider();
        for (OfflinePlayer target : targets) {
            UUID targetId = target.getUniqueId();

            provider.getBalance(targetId, currency).thenAccept(currentBalance -> {

                CompletableFuture<Boolean> transactionFuture;
                double newBalance;

                switch (action) {
                    case GIVE -> {
                        transactionFuture = provider.deposit(targetId, currency, amount).thenApply(v -> true);
                        newBalance = currentBalance + amount;
                    }
                    case REMOVE -> {
                        transactionFuture = provider.detract(targetId, currency, amount);
                        newBalance = currentBalance - amount;
                    }
                    case SET -> {
                        transactionFuture = provider.setBalance(targetId, currency, amount).thenApply(v -> true);
                        newBalance = amount;
                    }
                    default -> throw new IllegalStateException("Unexpected action: " + action);
                }

                transactionFuture.thenAccept(success -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (success) {
                        handleSuccess(sender, target, amount, currentBalance, newBalance, action, prefix, helper);
                    } else {
                        ChatUtils.send(sender, "&cError: Insufficient funds or transaction failed for " + target.getName() + ".", "%prefix%", prefix);
                    }
                })).exceptionally(ex -> {
                    plugin.getLogger().severe("Error processing /eco command: " + ex.getMessage());
                    return null;
                });
            });
        }
    }

    private void handleSuccess(CommandSender sender, OfflinePlayer target, double amount, double oldBalance, double newBalance, EcoAction action, String prefix, TransactionHelper helper) {
        UUID actorId = sender instanceof Player p ? p.getUniqueId() : null;

        helper.commitAsync(
                actorId,
                target.getUniqueId(),
                amount,
                oldBalance,
                newBalance,
                TransactionTypes.ADMIN_ADJUSTMENT,
                action.name().toLowerCase(),
                sender.getName(),
                target.getName()
        );

        String formattedAmount = plugin.getFormatUtils().formatBalance(amount);
        LanguageKeys senderKey = switch (action) {
            case GIVE -> LanguageKeys.GAVE_MONEY;
            case REMOVE -> LanguageKeys.REMOVED_MONEY;
            case SET -> LanguageKeys.ECO_SET_SUCCESS;
        };

        languageManager.send(sender, senderKey, "%prefix%", prefix, "%amount%", formattedAmount, "%target%", target.getName() != null ? target.getName() : "Unknown");

        if (action != EcoAction.SET && target.isOnline()) {
            Player onlineTarget = target.getPlayer();
            if (onlineTarget != null) {
                LanguageKeys targetKey = (action == EcoAction.GIVE) ? LanguageKeys.RECEIVED_MONEY : LanguageKeys.MONEY_REMOVED;
                languageManager.send(onlineTarget, targetKey, "%prefix%", prefix, "%amount%", formattedAmount, "%source%", sender.getName());
            }
        }
    }

    private boolean isInvalidInput(CommandSender sender, String target, Double amount) {
        if (target == null || target.isEmpty() || amount == null) {
            sendUsage(sender);
            return true;
        }
        return false;
    }

    private void sendUsage(CommandSender sender) {
        languageManager.send(sender, LanguageKeys.ECO_USAGE, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
    }

    private enum EcoAction {GIVE, SET, REMOVE}
}