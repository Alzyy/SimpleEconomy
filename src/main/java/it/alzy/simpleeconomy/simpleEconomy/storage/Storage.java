package it.alzy.simpleeconomy.simpleEconomy.storage;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;


public interface Storage {

    void save(UUID uuid, double balance);
    void saveSync(UUID uuid, double balance);
    CompletableFuture<Double> load(UUID player);
    void create(UUID player);
}
