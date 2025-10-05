package it.alzy.simpleeconomy.plugin.api.internal;

import it.alzy.simpleeconomy.api.EconomyProvider;
import it.alzy.simpleeconomy.api.TransactionResult;
import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import org.bukkit.Bukkit;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EconomyProviderImpl implements EconomyProvider {

    private final SimpleEconomy plugin;

    public EconomyProviderImpl(SimpleEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public CompletableFuture<Double> getBalance(UUID uuid) {
        return CompletableFuture.supplyAsync(plugin::getStorage)
                .thenCompose(storage -> storage.getBalance(uuid));
    }

    @Override
    public CompletableFuture<Void> setBalance(UUID uuid, double amount) {
        return CompletableFuture.runAsync(() -> plugin.getStorage().save(uuid, amount));
    }


    @Override
    public CompletableFuture<Void> deposit(UUID uuid, double amount) {
        return plugin.getStorage().getBalance(uuid)
            .thenCompose(current -> {
                double newBalance = current + amount;
                if(Bukkit.getPlayer(uuid) != null) {
                    plugin.getCacheMap().put(uuid, newBalance);
                }
                plugin.getStorage().save(uuid, newBalance);
                return CompletableFuture.completedFuture(null);
            });
    }

    @Override
    public CompletableFuture<Boolean> detract(UUID uuid, double amount) {
        return plugin.getStorage().getBalance(uuid)
            .thenCompose(current -> {
                if (current < amount) {
                    return CompletableFuture.completedFuture(false);
                }

                double newBalance = current - amount;

                return CompletableFuture.runAsync(() -> {
                    plugin.getStorage().save(uuid, newBalance);
                    if (Bukkit.getPlayer(uuid) != null) {
                        plugin.getCacheMap().put(uuid, newBalance);
                    }
                }).thenApply(v -> true);
        });
    }


    @Override
    public CompletableFuture<Boolean> hasAccount(UUID uuid) {
        return plugin.getStorage().hasAccount(uuid);
    }

    @Override
    public CompletableFuture<Boolean> hasEnough(UUID uuid, double amount) {
        return plugin.getStorage().getBalance(uuid)
                .thenApply(current -> current >= amount);
    }

    @Override
    public CompletableFuture<TransactionResult> transfer(UUID from, UUID to, double amount) {
        return plugin.getStorage().getBalance(from)
            .thenCompose(fromBalance -> {
                if (fromBalance < amount) {
                    return CompletableFuture.completedFuture(TransactionResult.INSUFFICIENT_FUNDS);
                }

                double newFrom = fromBalance - amount;

                return CompletableFuture.runAsync(() -> plugin.getStorage().save(from, newFrom))
                    .thenCompose(v -> plugin.getStorage().getBalance(to))
                    .thenCompose(toBalance -> {
                        double newTo = toBalance + amount;
                        return CompletableFuture.runAsync(() -> plugin.getStorage().save(to, newTo));
                    })
                    .thenApply(v -> TransactionResult.SUCCESS)
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        return TransactionResult.ERROR;
                    });
            });
    }
}
