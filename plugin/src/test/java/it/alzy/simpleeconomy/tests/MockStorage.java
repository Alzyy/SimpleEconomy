package it.alzy.simpleeconomy.tests;

import it.alzy.simpleeconomy.plugin.storage.Storage;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class MockStorage implements Storage {
    private final Map<UUID, Map<String, Double>> mockDB = new ConcurrentHashMap<>();

    @Override
    public void save(UUID uuid, String currency, double balance) {
        saveSync(uuid, currency, balance);
    }

    @Override
    public void saveSync(UUID uuid, String currency, double balance) {
        mockDB.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(currency, balance);
    }

    @Override
    public CompletableFuture<Map<String, Double>> load(UUID uuid) {
        Map<String, Double> balances = mockDB.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        if (!balances.containsKey("money")) {
            balances.put("money", 1000.0); 
        }
        return CompletableFuture.completedFuture(balances);
    }

    @Override
    public CompletableFuture<Double> getBalance(UUID uuid, String currency) {
        Map<String, Double> balances = mockDB.get(uuid);
        return CompletableFuture.completedFuture(balances != null ? balances.getOrDefault(currency, 1000.0) : 1000.0);
    }

    @Override
    public void create(UUID uuid) {
        saveSync(uuid, "money", 1000.0);
    }

    @Override
    public void bulkSave() {}

    @Override
    public void close() {}

    @Override
    public CompletableFuture<Boolean> hasAccount(UUID uuid) {
        return CompletableFuture.completedFuture(mockDB.containsKey(uuid));
    }

    @Override
    public Map<String, Double> getTopBalances(String currency, int limit) {
        return new ConcurrentHashMap<>();
    }

    @Override
    public Map<String, Double> getAllBalances(String currency) {
        return new ConcurrentHashMap<>();
    }

    @Override
    public void purge(int days) {}
}