package it.alzy.simpleeconomy.plugin.model;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.annotation.Optional;
import it.alzy.simpleeconomy.api.TransactionTypes;
import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.configurations.SettingsConfig;
import it.alzy.simpleeconomy.plugin.i18n.LanguageManager;
import it.alzy.simpleeconomy.plugin.i18n.enums.LanguageKeys;
import it.alzy.simpleeconomy.plugin.records.Transaction;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@CommandAlias("%currencyName")
public class DynamicCommand extends BaseCommand {

    private final VirtualCurrency currency;
    private final SimpleEconomy plugin = SimpleEconomy.getInstance();
    private final LanguageManager languageManager = plugin.getLanguageManager();

    public DynamicCommand(VirtualCurrency currency) {
        this.currency = currency;
    }

    @Default
    @CommandCompletion("@players")
    @Description("Check balance")
    public void onDefault(Player player, @Optional String targetName) {
        if (targetName == null || targetName.isEmpty()) {
            double bal = currency.getBalance(player.getUniqueId());
            languageManager.sendCurrencyMessage(player, currency, LanguageKeys.BALANCE_CHECK_SELF, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX), "%balance%", plugin.getFormatUtils().formatVirtualCurrencyBalance(currency, bal));
        } else {
            if (!player.hasPermission("simpleconomy.balance.others")) {
                languageManager.sendCurrencyMessage(player, currency, LanguageKeys.NO_PERMISSION, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
                return;
            }

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
                if (!target.hasPlayedBefore() && !target.isOnline()) {
                    languageManager.sendCurrencyMessage(player, currency, LanguageKeys.PLAYER_NOT_FOUND, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX), "%target%", targetName);
                    return;
                }

                double bal = currency.getBalance(target.getUniqueId());
                String formatted = plugin.getFormatUtils().formatVirtualCurrencyBalance(currency, bal);

                languageManager.sendCurrencyMessage(player, currency, LanguageKeys.BALANCE_CHECK_OTHER, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX), "%balance%", formatted, "%target%", target.getName());
            });
        }
    }

    @Subcommand("pay")
    @CommandCompletion("@players")
    @Description("Send money to another player")
    public void onPay(Player sender, String targetName, Double amount) {
        if (targetName == null || targetName.isEmpty() || amount == null || amount <= 0) {
            languageManager.sendCurrencyMessage(sender, currency, LanguageKeys.PAY_USAGE, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
            return;
        }

        if (!Double.isFinite(amount) || BigDecimal.valueOf(amount).scale() > 2) {
            languageManager.sendCurrencyMessage(sender, currency, LanguageKeys.INVALID_AMOUNT, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
            return;
        }

        if (sender.getName().equalsIgnoreCase(targetName)) {
            languageManager.sendCurrencyMessage(sender, currency, LanguageKeys.SELF_COMMAND, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
            return;
        }

        if (limited(amount)) {
            double max = SettingsConfig.getInstance().getMaxTransactionLimit();
            languageManager.sendCurrencyMessage(sender, currency, LanguageKeys.AMOUNT_EXCEEDS_LIMIT,
                    "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX),
                    "%max%", plugin.getFormatUtils().formatVirtualCurrencyBalance(currency, max));
            return;
        }

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            languageManager.sendCurrencyMessage(sender, currency, LanguageKeys.PLAYER_NOT_FOUND, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX), "%target%", targetName);
            return;
        }

        double senderBal = currency.getBalance(sender.getUniqueId());
        if (senderBal < amount) {
            languageManager.sendCurrencyMessage(sender, currency, LanguageKeys.NOT_ENOUGH_MONEY,
                    "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX),
                    "%balance%", plugin.getFormatUtils().formatVirtualCurrencyBalance(currency, senderBal));
            return;
        }

        UUID senderUUID = sender.getUniqueId();
        UUID targetUUID = target.getUniqueId();

        double senderBalanceAfter = senderBal - amount;
        double targetBalanceAfter = currency.getBalance(targetUUID) + amount;

        currency.setBalance(senderUUID, senderBalanceAfter);
        currency.setBalance(targetUUID, targetBalanceAfter);

        String formattedAmount = plugin.getFormatUtils().formatVirtualCurrencyBalance(currency, amount);

        languageManager.send(sender, LanguageKeys.GAVE_MONEY,
                "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX),
                "%amount%", formattedAmount,
                "%target%", target.getName());

        languageManager.send(target, LanguageKeys.RECEIVED_MONEY,
                "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX),
                "%amount%", formattedAmount,
                "%source%", sender.getName());

        plugin.getExecutor().execute(() -> {
            plugin.getStorage().save(senderUUID, currency.getName(), senderBalanceAfter);
            plugin.getStorage().save(targetUUID, currency.getName(), targetBalanceAfter);

            if (SettingsConfig.getInstance().isTransactionLoggingEnabled()) {
                Transaction transaction = new Transaction(senderUUID.toString(), targetUUID.toString(), currency.getName(), amount, senderBal, senderBalanceAfter, TransactionTypes.PAY, System.currentTimeMillis());
                plugin.getTransactionLogger().appendLog(transaction);
            }
            if (plugin.getWebhookLogger() != null) {
                plugin.getWebhookLogger().send("pay (" + currency.getName() + ")", target.getName(), sender.getName(), amount);
            }
        });
    }

    @Subcommand("give")
    @CommandPermission("simpleeconomy.eco.give")
    @CommandCompletion("@players|@a|@p|@r")
    public void give(CommandSender sender, @Optional String targetName, @Optional Double amount) {
        if (targetName == null || amount == null) {
            languageManager.sendCurrencyMessage(sender, currency, LanguageKeys.ECO_USAGE, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
            return;
        }
        executeAdminAction(sender, targetName, amount, EcoAction.GIVE);
    }

    @Subcommand("set")
    @CommandPermission("simpleeconomy.eco.set")
    @CommandCompletion("@players|@a|@p|@r")
    public void set(CommandSender sender, @Optional String targetName, @Optional Double amount) {
        if (targetName == null || amount == null) {
            languageManager.sendCurrencyMessage(sender, currency, LanguageKeys.ECO_USAGE, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
            return;
        }
        executeAdminAction(sender, targetName, amount, EcoAction.SET);
    }

    @Subcommand("remove")
    @CommandPermission("simpleeconomy.eco.remove")
    @CommandCompletion("@players|@a|@p|@r")
    public void remove(CommandSender sender, @Optional String targetName, @Optional Double amount) {
        if (targetName == null || amount == null) {
            languageManager.sendCurrencyMessage(sender, currency, LanguageKeys.ECO_USAGE, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
            return;
        }
        executeAdminAction(sender, targetName, amount, EcoAction.REMOVE);
    }

    private void executeAdminAction(CommandSender sender, String targetName, double amount, EcoAction action) {
        if (!isValidAmount(sender, amount, action)) return;

        Collection<OfflinePlayer> targets = resolveTargets(sender, targetName);
        if (targets.isEmpty()) return;

        final String formattedAmount = plugin.getFormatUtils().formatVirtualCurrencyBalance(currency, amount);

        for (OfflinePlayer target : targets) {
            double current = currency.getBalance(target.getUniqueId());
            double newBalance = switch (action) {
                case GIVE -> current + amount;
                case SET -> amount;
                case REMOVE -> Math.max(0, current - amount);
            };

            currency.setBalance(target.getUniqueId(), newBalance);
            handleAdminSuccess(sender, target, amount, current, newBalance, formattedAmount, action);
        }
    }

    private void handleAdminSuccess(CommandSender sender, OfflinePlayer target, double amount, double oldBalance, double newBalance, String formattedAmount, EcoAction action) {
        plugin.getExecutor().execute(() -> plugin.getStorage().save(target.getUniqueId(), currency.getName(), newBalance));

        LanguageKeys senderMsg = switch (action) {
            case GIVE -> LanguageKeys.GAVE_MONEY;
            case REMOVE -> LanguageKeys.REMOVED_MONEY;
            case SET -> LanguageKeys.ECO_SET_SUCCESS;
        };

        languageManager.sendCurrencyMessage(sender, currency, senderMsg, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX), "%amount%", formattedAmount, "%target%", target.getName());

        if (action != EcoAction.SET && target.isOnline() && target.getPlayer() != null) {
            LanguageKeys targetMsg = (action == EcoAction.GIVE) ? LanguageKeys.RECEIVED_MONEY : LanguageKeys.MONEY_REMOVED;
            languageManager.sendCurrencyMessage(target.getPlayer(), currency, targetMsg, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX), "%amount%", formattedAmount, "%source%", sender.getName());
        }

        if (SettingsConfig.getInstance().isTransactionLoggingEnabled()) {
            Transaction transaction = new Transaction(
                    sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "Console",
                    currency.getName(),
                    target.getUniqueId().toString(),
                    amount, oldBalance, newBalance,
                    TransactionTypes.ADMIN_ADJUSTMENT,
                    System.currentTimeMillis()
            );
            plugin.getTransactionLogger().appendLog(transaction);
        }

        if (plugin.getWebhookLogger() != null) {
            plugin.getWebhookLogger().send(action.name().toLowerCase() + " (" + currency.getName() + ")", target.getName(), sender.getName(), amount);
        }
    }

    private boolean isValidAmount(CommandSender sender, double amount, EcoAction action) {
        if (!Double.isFinite(amount)) {
            languageManager.sendCurrencyMessage(sender, currency, LanguageKeys.INVALID_AMOUNT, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
            return false;
        }
        if ((action == EcoAction.SET ? amount < 0 : amount <= 0) || BigDecimal.valueOf(amount).scale() > 2) {
            languageManager.sendCurrencyMessage(sender, currency, LanguageKeys.INVALID_AMOUNT, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
            return false;
        }
        return true;
    }

    private Collection<OfflinePlayer> resolveTargets(CommandSender sender, String targetName) {
        if (targetName.length() == 2 && targetName.charAt(0) == '@') {
            char selector = Character.toLowerCase(targetName.charAt(1));
            return switch (selector) {
                case 'a' -> new ArrayList<>(Bukkit.getOnlinePlayers());
                case 'p' -> (sender instanceof Player p) ? Collections.singletonList(p) : Collections.emptyList();
                case 'r' -> {
                    List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
                    if (online.isEmpty()) yield Collections.emptyList();
                    yield Collections.singletonList(online.get(ThreadLocalRandom.current().nextInt(online.size())));
                }
                default -> getOfflinePlayerFallback(sender, targetName);
            };
        }
        return getOfflinePlayerFallback(sender, targetName);
    }

    private Collection<OfflinePlayer> getOfflinePlayerFallback(CommandSender sender, String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            languageManager.sendCurrencyMessage(sender, currency, LanguageKeys.PLAYER_NOT_FOUND, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
            return Collections.emptyList();
        }
        return Collections.singletonList(target);
    }

    private boolean limited(double amount) {
        double max = SettingsConfig.getInstance().getMaxTransactionLimit();
        return max != 0 && amount > max;
    }

    private enum EcoAction {GIVE, SET, REMOVE}
}