package it.alzy.simpleeconomy.simpleEconomy.storage;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


public interface Storage {

    void save(UUID uuid, double balance);
    void saveSync(UUID uuid, double balance);
    CompletableFuture<Double> load(UUID player);
    void create(UUID player);
    CompletableFuture<Boolean> hasAccount(UUID uuid);
    Map<String, Double> getTopBalances(int limit);
    void bulkSave();
    void close();
}
