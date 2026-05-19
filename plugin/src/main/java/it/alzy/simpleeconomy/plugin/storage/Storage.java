package it.alzy.simpleeconomy.plugin.storage;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface Storage {
    void save(UUID uuid, String currency, double balance);
    void saveSync(UUID uuid, String currency, double balance);
    CompletableFuture<Map<String, Double>> load(UUID player);
    void create(UUID player);
    CompletableFuture<Boolean> hasAccount(UUID uuid);
    CompletableFuture<Double> getBalance(UUID uuid, String currency);
    Map<String, Double> getTopBalances(String currency, int limit);
    void bulkSave();
    void close();
    void purge(int days);
    Map<String, Double> getAllBalances(String currency);
    
}