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
import org.bukkit.event.Event;

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
        double rounded = plugin.getDoubleUtils().round(amount);
        return CompletableFuture.runAsync(() -> {
            plugin.getStorage().save(uuid, currency, rounded);

            if (plugin.getCache().contains(uuid)) {
                plugin.getCache().updateCurrency(uuid, currency, rounded);
            }
        }, plugin.getExecutor());
    }

    @Override
    public CompletableFuture<Void> deposit(UUID uuid, String currency, double amount) {
        double roundedAmount = plugin.getDoubleUtils().round(amount);
        PreTransactionEvent preEvent = new PreTransactionEvent(null, uuid, currency, roundedAmount, TransactionTypes.DEPOSIT);

        return callEventAsync(preEvent).thenCompose(v -> {
            if (preEvent.isCancelled()) {
                throw new IllegalStateException("Transaction cancelled: " + preEvent.getCancelReason());
            }
            return getBalance(uuid, currency);
        }).thenCompose(current -> {
            double newBalance = current + roundedAmount;
            return updateBalanceInternal(uuid, currency, newBalance);
        }).thenCompose(v -> {
            PostTransactionEvent postEvent = new PostTransactionEvent(null, uuid, currency, roundedAmount, TransactionTypes.DEPOSIT);
            return callEventAsync(postEvent);
        });
    }

    @Override
    public CompletableFuture<Boolean> detract(UUID uuid, String currency, double amount) {
        double roundedAmount = plugin.getDoubleUtils().round(amount);
        PreTransactionEvent preEvent = new PreTransactionEvent(uuid, null, currency, roundedAmount, TransactionTypes.WITHDRAW);

        return callEventAsync(preEvent).thenCompose(v -> {
            if (preEvent.isCancelled()) {
                return CompletableFuture.completedFuture(false);
            }

            return getBalance(uuid, currency).thenCompose(current -> {
                if (current < roundedAmount) {
                    return CompletableFuture.completedFuture(false);
                }

                double newBalance = current - roundedAmount;
                return updateBalanceInternal(uuid, currency, newBalance).thenCompose(v2 -> {
                    PostTransactionEvent postEvent = new PostTransactionEvent(uuid, null, currency, roundedAmount, TransactionTypes.WITHDRAW);
                    return callEventAsync(postEvent).thenApply(v3 -> true);
                });
            });
        });
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
        return getBalance(from, currency).thenCompose(fromBalance -> {
            if (fromBalance < amount) {
                return CompletableFuture.completedFuture(TransactionResult.INSUFFICIENT_FUNDS);
            }

            double roundedAmount = plugin.getDoubleUtils().round(amount);
            PreTransactionEvent preEvent = new PreTransactionEvent(from, to, currency, roundedAmount, TransactionTypes.PAY);

            return callEventAsync(preEvent).thenCompose(v -> {
                if (preEvent.isCancelled()) {
                    return CompletableFuture.completedFuture(TransactionResult.ERROR);
                }

                return detract(from, currency, roundedAmount).thenCompose(success -> {
                    if (!success) return CompletableFuture.completedFuture(TransactionResult.ERROR);

                    return deposit(to, currency, roundedAmount).thenCompose(depV -> {
                        PostTransactionEvent postEvent = new PostTransactionEvent(from, to, currency, roundedAmount, TransactionTypes.PAY);
                        return callEventAsync(postEvent).thenApply(postV -> TransactionResult.SUCCESS);
                    }).exceptionallyCompose(ex -> {
                        plugin.getLogger().severe("Transfer failed during deposit to " + to + ". Refunding " + from);
                        return deposit(from, currency, roundedAmount).thenApply(refundV -> TransactionResult.ERROR);
                    });
                });
            });
        });
    }

    @Override
    public Set<String> getAvailableVirtualCurrencies() {
        return plugin.getCurrencyManager().getAllCurrencies().stream().map(VirtualCurrency::getName).collect(Collectors.toSet());
    }

    private CompletableFuture<Void> updateBalanceInternal(UUID uuid, String currency, double newBalance) {
        double roundedBalance = plugin.getDoubleUtils().round(newBalance);
        return CompletableFuture.runAsync(() -> {
            plugin.getStorage().save(uuid, currency, roundedBalance);

            if (plugin.getCache().contains(uuid)) {
                plugin.getCache().updateCurrency(uuid, currency, roundedBalance);
            }
        }, plugin.getExecutor());
    }

    private CompletableFuture<Void> callEventAsync(Event ev) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Bukkit.getPluginManager().callEvent(ev);
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }
}