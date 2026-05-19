package it.alzy.simpleeconomy.plugin.api.internal;

import it.alzy.simpleeconomy.api.EconomyProvider;
import it.alzy.simpleeconomy.api.TransactionResult;
import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.configurations.SettingsConfig;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class EconomyProviderImpl implements EconomyProvider {

    private final SimpleEconomy plugin;

    public EconomyProviderImpl(SimpleEconomy plugin) {
        this.plugin = plugin;
    }

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
        });
    }

    @Override
    public CompletableFuture<Void> deposit(UUID uuid, String currency, double amount) {
        return getBalance(uuid, currency).thenCompose(current -> {
            double newBalance = current + amount;
            return updateBalanceInternal(uuid, currency, newBalance);
        });
    }

    @Override
    public CompletableFuture<Boolean> detract(UUID uuid, String currency, double amount) {
        return getBalance(uuid, currency).thenCompose(current -> {
            if (current < amount) {
                return CompletableFuture.completedFuture(false);
            }
            double newBalance = current - amount;
            return updateBalanceInternal(uuid, currency, newBalance).thenApply(v -> true);
        });
    }

    private CompletableFuture<Void> updateBalanceInternal(UUID uuid, String currency, double newBalance) {
        return CompletableFuture.runAsync(() -> {
            plugin.getStorage().save(uuid, currency, newBalance);
            
            if (plugin.getCache().contains(uuid)) {
                plugin.getCache().updateCurrency(uuid, currency, newBalance);
            }
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

            return detract(from, currency, amount)
            .thenCompose(success -> {
                if(!success) return CompletableFuture.completedFuture(TransactionResult.ERROR);
                
                return deposit(to, currency, amount).exceptionallyCompose(ex -> {
                    plugin.getLogger().severe("Transfer failed during deposit to " + to + ". Refunding " + from);
                    return deposit(from, currency, amount);
                })
                .thenApply(v -> TransactionResult.SUCCESS);
            });
        });
    }

    @Override
    public Set<String> getAvailableVirtualCurrencies() {
        return plugin.getCurrencyManager().getAllCurrencies().stream().map(curr -> curr.getName()).collect(Collectors.toSet());
    }
}