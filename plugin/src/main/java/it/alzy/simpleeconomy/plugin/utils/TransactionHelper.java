package it.alzy.simpleeconomy.plugin.utils;

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

public final class TransactionHelper {

    private final SimpleEconomy plugin;
    private final LanguageManager lang;

    public TransactionHelper(SimpleEconomy plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
    }

    public Collection<OfflinePlayer> resolveTargets(CommandSender sender, String input) {
        if (input.startsWith("@") && input.length() == 2) {
            return switch (Character.toLowerCase(input.charAt(1))) {
                case 'a' -> new ArrayList<>(Bukkit.getOnlinePlayers());
                case 'p' -> sender instanceof Player p
                        ? Collections.singletonList(p)
                        : Collections.emptyList();
                case 'r' -> {
                    List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
                    yield online.isEmpty()
                            ? Collections.emptyList()
                            : Collections.singletonList(online.get(ThreadLocalRandom.current().nextInt(online.size())));
                }
                default -> resolveSingleOffline(sender, input);
            };
        }
        return resolveSingleOffline(sender, input);
    }

    public Optional<Player> resolveOnlineTarget(CommandSender sender, String name) {
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            sendPrefixed(sender, LanguageKeys.PLAYER_NOT_FOUND);
            return Optional.empty();
        }
        return Optional.of(target);
    }

    private Collection<OfflinePlayer> resolveSingleOffline(CommandSender sender, String name) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(name);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sendPrefixed(sender, LanguageKeys.PLAYER_NOT_FOUND);
            return Collections.emptyList();
        }
        return Collections.singletonList(target);
    }

    public boolean validateAmount(CommandSender sender, double amount) {
        return validateAmount(sender, amount, false);
    }

    public boolean validateAmount(CommandSender sender, double amount, boolean allowZero) {
        boolean positiveCheck = allowZero ? amount >= 0 : amount > 0;
        if (!Double.isFinite(amount) || !positiveCheck || BigDecimal.valueOf(amount).scale() > 2) {
            sendPrefixed(sender, LanguageKeys.INVALID_AMOUNT);
            return false;
        }
        return true;
    }

    public boolean checkTransactionLimit(CommandSender sender, double amount) {
        double max = SettingsConfig.getInstance().getMaxTransactionLimit();
        if (max > 0 && amount > max) {
            lang.send(sender, LanguageKeys.AMOUNT_EXCEEDS_LIMIT,
                    "%prefix%", lang.getMessage(LanguageKeys.PREFIX),
                    "%max%", plugin.getFormatUtils().formatBalance(max));
            return false;
        }
        return true;
    }

    public void commitAsync(UUID actorId, UUID targetId, double amount, double balanceBefore, double balanceAfter, TransactionTypes type, String webhookAction, String webhookActor, String webhookTarget) {
        commitAsync(actorId, targetId, "money", amount, balanceBefore, balanceAfter, type, webhookAction, webhookActor, webhookTarget);
    }

    public void commitAsync( UUID actorId, UUID targetId, String currency, double amount, double balanceBefore, double balanceAfter, TransactionTypes type,String webhookAction, String webhookActor, String webhookTarget ) {
        plugin.getCache().updateCurrency(targetId, currency, balanceAfter);

        plugin.getExecutor().execute(() -> {
            plugin.getStorage().save(targetId, currency, balanceAfter);

            if (SettingsConfig.getInstance().isTransactionLoggingEnabled()) {
                String actorStr = actorId != null ? actorId.toString() : "Console";
                Transaction tx = new Transaction(
                        actorStr,
                        targetId.toString(),
                        currency,
                        amount,
                        balanceBefore,
                        balanceAfter,
                        type,
                        System.currentTimeMillis()
                );
                plugin.getTransactionLogger().appendLog(tx);
            }

            if (plugin.getWebhookLogger() != null) {
                plugin.getWebhookLogger().send(webhookAction + " (" + currency + ")", webhookTarget, webhookActor, amount);
            }
        });
    }


    public void commitTransferAsync(UUID senderId, UUID recipientId, String currency,double amount, double senderAfter, double recipientAfter, String senderName,String recipientName ) {
        plugin.getCache().updateCurrency(senderId, currency, senderAfter);
        plugin.getCache().updateCurrency(recipientId, currency, recipientAfter);

        plugin.getExecutor().execute(() -> {
            plugin.getStorage().save(senderId, currency, senderAfter);
            plugin.getStorage().save(recipientId, currency, recipientAfter);

            if (SettingsConfig.getInstance().isTransactionLoggingEnabled()) {
                Transaction tx = new Transaction(
                        senderId.toString(),
                        recipientId.toString(),
                        currency,
                        amount,
                        senderAfter + amount, 
                        senderAfter,         
                        TransactionTypes.PAY,
                        System.currentTimeMillis()
                );
                plugin.getTransactionLogger().appendLog(tx);
            }

            if (plugin.getWebhookLogger() != null) {
                plugin.getWebhookLogger().send("pay (" + currency + ")", recipientName, senderName, amount);
            }
        });
    }

    private void sendPrefixed(CommandSender sender, LanguageKeys key) {
        lang.send(sender, key, "%prefix%", lang.getMessage(LanguageKeys.PREFIX));
    }
}