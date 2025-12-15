package it.alzy.simpleeconomy.plugin.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.annotation.Optional;
import it.alzy.simpleeconomy.api.TransactionTypes;
import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.configurations.SettingsConfig;
import it.alzy.simpleeconomy.plugin.i18n.LanguageManager;
import it.alzy.simpleeconomy.plugin.i18n.enums.LanguageKeys;
import it.alzy.simpleeconomy.plugin.records.Transaction;
import it.alzy.simpleeconomy.plugin.utils.ChatUtils;
import it.alzy.simpleeconomy.plugin.utils.VaultHook;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;


@CommandAlias("eco")
@Description("Manage players' economy balances.")
public class ECOCommand extends BaseCommand {

    private final SimpleEconomy plugin;
    private final LanguageManager languageManager;

    public ECOCommand() {
        this.plugin = SimpleEconomy.getInstance();
        this.languageManager = plugin.getLanguageManager();
    }

    private enum EcoAction { GIVE, SET, REMOVE }

    @Default
    public void root(CommandSender player) {
        languageManager.send(player, LanguageKeys.ECO_USAGE, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
    }


    @Subcommand("set")
    @CommandCompletion("@players|@a|@p|@r")
    public void set(CommandSender sender, @Optional String targetName, @Optional Double amount) {
        if(targetName.isEmpty() || amount == null) {
            languageManager.send(sender, LanguageKeys.ECO_USAGE, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
            return;
        }

        execute(sender, targetName, amount, EcoAction.SET);
    }

    @Subcommand("give")
    @CommandCompletion("@players|@a|@p|@r")
    public void give(CommandSender sender, @Optional String targetName, @Optional Double amount) {
        if(targetName.isEmpty() || amount == null) {
            languageManager.send(sender, LanguageKeys.ECO_USAGE, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
            return;
        }
        execute(sender, targetName, amount, EcoAction.GIVE);
    }

    @Subcommand("remove")
    @CommandCompletion("@players|@a|@p|@r")
    public void remove(CommandSender sender, @Optional String targetName, @Optional Double amount) {
        if(targetName.isEmpty() || amount == null) {
            languageManager.send(sender, LanguageKeys.ECO_USAGE, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
            return;
        }
        execute(sender, targetName, amount, EcoAction.REMOVE);
    }

    private void execute(CommandSender sender, String targetName, double amount, EcoAction action) {
        if (!sender.hasPermission("simpleconomy.eco." + action.name().toLowerCase())) {
            languageManager.send(sender, LanguageKeys.NO_PERMISSION, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
            return;
        }

        if (!isValidAmount(sender, amount, action)) return;

        Economy economy = VaultHook.getEconomy();
        if (economy == null) {
            return;
        }

        Collection<OfflinePlayer> targets = resolveTargets(sender, targetName);
        if (targets.isEmpty()) return;

        final String formattedAmount = plugin.getFormatUtils().formatBalance(amount);

        for (OfflinePlayer target : targets) {
            EconomyResponse response;

            switch (action) {
                case GIVE -> response = economy.depositPlayer(target, amount);

                case REMOVE -> {
                    if (!economy.has(target, amount)) {
                        languageManager.send(sender, LanguageKeys.NOT_ENOUGH_MONEY, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
                        continue;
                    }
                    response = economy.withdrawPlayer(target, amount);
                }

                case SET -> {
                    double current = economy.getBalance(target);
                    if (current < amount) {
                        response = economy.depositPlayer(target, amount - current);
                    } else if (current > amount) {
                        response = economy.withdrawPlayer(target, current - amount);
                    } else {
                        response = new EconomyResponse(0, current, EconomyResponse.ResponseType.SUCCESS, null);
                    }
                }
                default -> response = new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Unknown Error");
            }

            if (response.transactionSuccess()) {
                handleSuccess(sender, target, response.balance, formattedAmount, action);
            } else {
                ChatUtils.send(sender, "&cError: " + response.errorMessage, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
            }
        }
    }

    private void handleSuccess(CommandSender sender, OfflinePlayer target, double newBalance, String formattedAmount, EcoAction action) {
        plugin.getCacheMap().put(target.getUniqueId(), newBalance);
        plugin.getExecutor().execute(() -> plugin.getStorage().save(target.getUniqueId(), newBalance));

        LanguageKeys senderMsg = switch (action) {
            case GIVE -> LanguageKeys.GAVE_MONEY;
            case REMOVE -> LanguageKeys.REMOVED_MONEY;
            case SET -> LanguageKeys.ECO_SET_SUCCESS;
        };

        languageManager.send(sender, senderMsg, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX), "%amount%", formattedAmount, "%target%", target.getName());

        if (action != EcoAction.SET && target.isOnline() && target.getPlayer() != null) {
            LanguageKeys targetMsg = (action == EcoAction.GIVE) ? LanguageKeys.RECEIVED_MONEY: LanguageKeys.MONEY_REMOVED;
            languageManager.send(target.getPlayer(), targetMsg,"%prefix%", languageManager.getMessage(LanguageKeys.PREFIX), "%amount%", formattedAmount, "%source%", sender.getName());
        }

        if(SettingsConfig.getInstance().isFormattingEnabled()) {
            double balanceBefore = switch (action) {
                case GIVE -> newBalance - Double.parseDouble(formattedAmount.replaceAll("[^\\d.]", ""));
                case REMOVE -> newBalance + Double.parseDouble(formattedAmount.replaceAll("[^\\d.]", ""));
                case SET -> VaultHook.getEconomy().getBalance(target);
            };

            double amountValue = Double.parseDouble(formattedAmount.replaceAll("[^\\d.]", ""));

            Transaction transaction = new Transaction(
                    sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "Console",
                    target.getUniqueId().toString(),
                    amountValue,
                    balanceBefore,
                    newBalance,
                    TransactionTypes.ADMIN_ADJUSTMENT,
                    System.currentTimeMillis()
            );

            plugin.getTransactionLogger().appendLog(transaction);
        }
    }

    private boolean isValidAmount(CommandSender sender, double amount, EcoAction action) {
        if (!Double.isFinite(amount)) {
            languageManager.send(sender, LanguageKeys.INVALID_AMOUNT,"%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
            return false;
        }

        boolean isInvalid = (action == EcoAction.SET) ? (amount < 0) : (amount <= 0);

        if (isInvalid) {
            languageManager.send(sender, LanguageKeys.INVALID_AMOUNT,"%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
            return false;
        }

        if (BigDecimal.valueOf(amount).scale() > 2) {
            languageManager.send(sender, LanguageKeys.INVALID_AMOUNT,"%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
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
                    Collection<? extends Player> online = Bukkit.getOnlinePlayers();
                    if (online.isEmpty()) yield Collections.emptyList();
                    yield Collections.singletonList(online.stream()
                            .skip(ThreadLocalRandom.current().nextInt(online.size()))
                            .findFirst().orElse(null));
                }
                default -> getOfflinePlayerFallback(sender, targetName);
            };
        }
        return getOfflinePlayerFallback(sender, targetName);
    }

    private Collection<OfflinePlayer> getOfflinePlayerFallback(CommandSender sender, String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            languageManager.send(sender, LanguageKeys.PLAYER_NOT_FOUND,"%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
            return Collections.emptyList();
        }
        return Collections.singletonList(target);
    }
}