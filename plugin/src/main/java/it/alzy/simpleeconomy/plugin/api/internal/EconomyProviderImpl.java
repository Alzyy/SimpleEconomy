package it.alzy.simpleeconomy.plugin.api.internal;

import it.alzy.simpleeconomy.api.EconomyProvider;
import it.alzy.simpleeconomy.api.TransactionResult;
import it.alzy.simpleeconomy.api.TransactionTypes;
import it.alzy.simpleeconomy.api.events.PostTransactionEvent;
import it.alzy.simpleeconomy.api.events.PreTransactionEvent;
import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.configurations.SettingsConfig;
import it.alzy.simpleeconomy.plugin.model.VirtualCurrency;
import org.bukkit.Bukkit;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public record EconomyProviderImpl(SimpleEconomy plugin) implements EconomyProvider {

    @Override
    public CompletableFuture<Double> getBalance(UUID uuid, String currency) {
        if (plugin.getCache().contains(uuid)) {
            Map<String, Double> balances = plugin.getCache().get(uuid);
            if (balances != null && balances.containsKey(currency)) {
                return CompletableFuture.completedFuture(balances.get(currency));
            } else {
                return CompletableFuture.completedFuture(SettingsConfig.getInstance().startingBalance());
            }
        }
        return plugin.getStorage().getBalance(uuid, currency);
    }

    @Override
    public CompletableFuture<Void> setBalance(UUID uuid, String currency, double amount) {
        return CompletableFuture.runAsync(() -> {
            plugin.getStorage().save(uuid, currency, amount);

            if (plugin.getCache().contains(uuid)) {
                plugin.getCache().updateCurrency(uuid, currency, amount);
            }
        }, plugin.getExecutor());
    }

    @Override
    public CompletableFuture<Void> deposit(UUID uuid, String currency, double amount) {
        // Spostiamo subito l'esecuzione in asincrono prima di triggerare il PreTransactionEvent
        return CompletableFuture.runAsync(() -> {
            PreTransactionEvent preEvent = new PreTransactionEvent(null, uuid, currency, amount, TransactionTypes.DEPOSIT);
            Bukkit.getPluginManager().callEvent(preEvent);

            if (preEvent.isCancelled()) {
                throw new IllegalStateException("Transaction cancelled: " + preEvent.getCancelReason());
            }
        }, plugin.getExecutor())
        // Usiamo thenComposeAsync per agganciare il getBalance garantendo l'asincronia
        .thenComposeAsync(v -> getBalance(uuid, currency), plugin.getExecutor())
        .thenComposeAsync(current -> {
            double newBalance = current + amount;
            return updateBalanceInternal(uuid, currency, newBalance).thenRunAsync(() -> {
                PostTransactionEvent postEvent = new PostTransactionEvent(null, uuid, currency, amount, TransactionTypes.DEPOSIT);
                Bukkit.getPluginManager().callEvent(postEvent);
            }, plugin.getExecutor());
        }, plugin.getExecutor());
    }

    @Override
    public CompletableFuture<Boolean> detract(UUID uuid, String currency, double amount) {
        // Spostiamo anche qui il PreTransactionEvent dentro l'executor asincrono
        return CompletableFuture.supplyAsync(() -> {
            PreTransactionEvent preEvent = new PreTransactionEvent(uuid, null, currency, amount, TransactionTypes.WITHDRAW);
            Bukkit.getPluginManager().callEvent(preEvent);
            return !preEvent.isCancelled();
        }, plugin.getExecutor()).thenComposeAsync(allowed -> {
            if (!allowed) return CompletableFuture.completedFuture(false);

            return getBalance(uuid, currency).thenComposeAsync(current -> {
                if (current < amount) {
                    return CompletableFuture.completedFuture(false);
                }
                double newBalance = current - amount;
                return updateBalanceInternal(uuid, currency, newBalance).thenApplyAsync(v -> {
                    PostTransactionEvent postEvent = new PostTransactionEvent(uuid, null, currency, amount, TransactionTypes.WITHDRAW);
                    Bukkit.getPluginManager().callEvent(postEvent);
                    return true;
                }, plugin.getExecutor());
            }, plugin.getExecutor());
        }, plugin.getExecutor());
    }

    private CompletableFuture<Void> updateBalanceInternal(UUID uuid, String currency, double newBalance) {
        return CompletableFuture.runAsync(() -> {
            plugin.getStorage().save(uuid, currency, newBalance);

            if (plugin.getCache().contains(uuid)) {
                plugin.getCache().updateCurrency(uuid, currency, newBalance);
            }
        }, plugin.getExecutor());
    }

    @Override
    public CompletableFuture<Boolean> hasAccount(UUID uuid) {
        return plugin.getStorage().hasAccount(uuid);
    }

    @Override
    public CompletableFuture<Boolean> hasEnough(UUID uuid, String currency, double amount) {
        return getBalance(uuid, currency).thenApply(current -> current >= amount);
    }

    @Override
    public CompletableFuture<TransactionResult> transfer(UUID from, UUID to, String currency, double amount) {
        return getBalance(from, currency).thenComposeAsync(fromBalance -> {
            if (fromBalance < amount) {
                return CompletableFuture.completedFuture(TransactionResult.INSUFFICIENT_FUNDS);
            }

            // Spostiamo anche la transazione intera sotto l'esecutore asincrono
            return CompletableFuture.supplyAsync(() -> {
                PreTransactionEvent preEvent = new PreTransactionEvent(from, to, currency, amount, TransactionTypes.PAY);
                Bukkit.getPluginManager().callEvent(preEvent);
                return !preEvent.isCancelled();
            }, plugin.getExecutor()).thenComposeAsync(allowed -> {
                if (!allowed) return CompletableFuture.completedFuture(TransactionResult.ERROR);

                return detract(from, currency, amount)
                    .thenComposeAsync(success -> {
                        if (!success) return CompletableFuture.completedFuture(TransactionResult.ERROR);

                        return deposit(to, currency, amount)
                            .thenApplyAsync(v -> {
                                PostTransactionEvent postEvent = new PostTransactionEvent(from, to, currency, amount, TransactionTypes.PAY);
                                Bukkit.getPluginManager().callEvent(postEvent);
                                return TransactionResult.SUCCESS;
                            }, plugin.getExecutor())
                            .exceptionallyCompose(ex -> {
                                plugin.getLogger().severe("Transfer failed during deposit to " + to + ". Refunding " + from);
                                return deposit(from, currency, amount).thenApply(v -> TransactionResult.ERROR);
                            });
                    }, plugin.getExecutor());
            }, plugin.getExecutor());
        }, plugin.getExecutor());
    }

    @Override
    public Set<String> getAvailableVirtualCurrencies() {
        return plugin.getCurrencyManager().getAllCurrencies().stream().map(VirtualCurrency::getName).collect(Collectors.toSet());
    }
}