package it.alzy.simpleeconomy.plugin.storage;

import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class Cache {

    private final com.github.benmanes.caffeine.cache.Cache<UUID, Map<String, Double>> cache;
    
    private final Set<UUID> dirtyPlayers;

    public Cache() {
        this.cache = Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .build();
                
        this.dirtyPlayers = ConcurrentHashMap.newKeySet();
    }

    public void put(UUID uuid, Map<String, Double> balances) {
        if (!(balances instanceof ConcurrentHashMap)) {
            balances = new ConcurrentHashMap<>(balances);
        }
        cache.put(uuid, balances);
        dirtyPlayers.add(uuid);
    }

    public Map<String, Double> get(UUID uuid) {
        return cache.getIfPresent(uuid);
    }

    public boolean contains(UUID uuid) {
        return cache.getIfPresent(uuid) != null;
    }

    public void remove(UUID uuid) {
        cache.invalidate(uuid);
        dirtyPlayers.remove(uuid);
    }

    public void updateCurrency(UUID uuid, String currency, double amount) {
        Map<String, Double> balances = cache.getIfPresent(uuid);
        if (balances != null) {
            balances.put(currency, amount);
            dirtyPlayers.add(uuid);
        }
    }

    public Map<UUID, Map<String, Double>> getAll() {
        return cache.asMap();
    }

    public void invalidateAll() {
        cache.invalidateAll();
        dirtyPlayers.clear();
    }

    public Set<UUID> getAndClearDirtyPlayers() {
        Set<UUID> snapshot;
        synchronized (dirtyPlayers) {
            snapshot = new HashSet<>(dirtyPlayers);
            dirtyPlayers.clear();
        }
        return snapshot;
    }
}