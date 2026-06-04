package it.alzy.simpleeconomy.plugin.storage;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class Cache {

    private final com.github.benmanes.caffeine.cache.Cache<@NotNull UUID, Map<String, Double>> cache;

    private final Set<UUID> dirtyPlayers = new HashSet<>();

    private final ReentrantLock lock = new ReentrantLock();

    public Cache() {
        this.cache = Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .build();

    }

    public void put(UUID uuid, Map<String, Double> balances) {
        lock.lock();
        try {
            if (!(balances instanceof ConcurrentHashMap)) {
                balances = new ConcurrentHashMap<>(balances);
            }
            cache.put(uuid, balances);
            dirtyPlayers.add(uuid);
        } finally {
            lock.unlock();
        }
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
        lock.lock();
        try {
            Map<String, Double> balances = cache.getIfPresent(uuid);
            if (balances != null) {
                balances.put(currency, amount);
                dirtyPlayers.add(uuid);
            }
        } finally {
            lock.unlock();
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
        lock.lock();
        try {
            Set<UUID> snapshot = new HashSet<>(dirtyPlayers);
            dirtyPlayers.clear();
            return snapshot;
        } finally {
            lock.unlock();
        }
    }

    public int getDirtySize() {
        return dirtyPlayers.size();
    }
}