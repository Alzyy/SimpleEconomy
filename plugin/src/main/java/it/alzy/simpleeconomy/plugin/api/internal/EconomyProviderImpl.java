package it.alzy.simpleeconomy.plugin.api.internal;

import it.alzy.simpleeconomy.api.EconomyProvider;
import it.alzy.simpleeconomy.api.TransactionResult;
import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import org.bukkit.Bukkit;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public record EconomyProviderImpl(SimpleEconomy instance) implements EconomyProvider {

    @Override
    public CompletableFuture<Double> getBalance(UUID uuid) {
        return CompletableFuture.supplyAsync(instance::getStorage)
                .thenCompose(storage -> storage.getBalance(uuid));
    }

    @Override
    public CompletableFuture<Void> setBalance(UUID uuid, double amount) {
        return CompletableFuture.runAsync(() -> instance.getStorage().save(uuid, amount));
    }


    @Override
    public CompletableFuture<Void> deposit(UUID uuid, double amount) {
        return instance.getStorage().getBalance(uuid)
                .thenCompose(current -> {
                    double newBalance = current + amount;
                    if (Bukkit.getPlayer(uuid) != null) {
                        instance.getCacheMap().put(uuid, newBalance);
                    }
                    instance.getStorage().save(uuid, newBalance);
                    return CompletableFuture.completedFuture(null);
                });
    }

    @Override
    public CompletableFuture<Boolean> detract(UUID uuid, double amount) {
        return instance.getStorage().getBalance(uuid)
                .thenCompose(current -> {
                    if (current < amount) {
                        return CompletableFuture.completedFuture(false);
                    }

                    double newBalance = current - amount;

                    return CompletableFuture.runAsync(() -> {
                        instance.getStorage().save(uuid, newBalance);
                        if (Bukkit.getPlayer(uuid) != null) {
                            instance.getCacheMap().put(uuid, newBalance);
                        }
                    }).thenApply(v -> true);
                });
    }


    @Override
    public CompletableFuture<Boolean> hasAccount(UUID uuid) {
        return instance.getStorage().hasAccount(uuid);
    }

    @Override
    public CompletableFuture<Boolean> hasEnough(UUID uuid, double amount) {
        return instance.getStorage().getBalance(uuid)
                .thenApply(current -> current >= amount);
    }

    @Override
    public CompletableFuture<TransactionResult> transfer(UUID from, UUID to, double amount) {
        return instance.getStorage().getBalance(from)
                .thenCompose(fromBalance -> {
                    if (fromBalance < amount) {
                        return CompletableFuture.completedFuture(TransactionResult.INSUFFICIENT_FUNDS);
                    }

                    double newFrom = fromBalance - amount;

                    return CompletableFuture.runAsync(() -> instance.getStorage().save(from, newFrom))
                            .thenCompose(v -> instance.getStorage().getBalance(to))
                            .thenCompose(toBalance -> {
                                double newTo = toBalance + amount;
                                return CompletableFuture.runAsync(() -> instance.getStorage().save(to, newTo));
                            })
                            .thenApply(v -> TransactionResult.SUCCESS)
                            .exceptionally(ex -> {
                                instance.getLogger().severe(ex.getMessage());
                                return TransactionResult.ERROR;
                            });
                });
    }
}
