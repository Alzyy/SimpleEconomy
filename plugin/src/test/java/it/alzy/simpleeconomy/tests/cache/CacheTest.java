package it.alzy.simpleeconomy.tests.cache;

import it.alzy.simpleeconomy.plugin.storage.Cache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

public class CacheTest {

    @Test
    @DisplayName("Stores balances in a concurrent map and marks player dirty")
    void testPutMarksDirtyAndStoresConcurrentMap() {
        Cache cache = new Cache();
        UUID uuid = UUID.randomUUID();
        Map<String, Double> balances = new HashMap<>();
        balances.put("money", 100.0);

        cache.put(uuid, balances);

        Map<String, Double> cached = cache.get(uuid);
        assertNotNull(cached, "Cached balances should be present");
        assertTrue(cached instanceof ConcurrentHashMap, "Cached balances should be a ConcurrentHashMap");

        Set<UUID> dirtyPlayers = cache.getAndClearDirtyPlayers();
        assertTrue(dirtyPlayers.contains(uuid), "Player should be marked dirty after cache put");
    }

    @Test
    @DisplayName("Updates currency and marks player dirty")
    void testUpdateCurrencyMarksDirty() {
        Cache cache = new Cache();
        UUID uuid = UUID.randomUUID();
        Map<String, Double> balances = new HashMap<>();
        balances.put("money", 50.0);
        cache.put(uuid, balances);

        cache.getAndClearDirtyPlayers();

        cache.updateCurrency(uuid, "money", 75.0);
        assertEquals(75.0, cache.get(uuid).get("money"), "Balance should be updated in cache");

        Set<UUID> dirtyPlayers = cache.getAndClearDirtyPlayers();
        assertTrue(dirtyPlayers.contains(uuid), "Player should be marked dirty after update");
    }

    @Test
    @DisplayName("Clears dirty tracking after retrieval")
    void testGetAndClearDirtyPlayersClears() {
        Cache cache = new Cache();
        UUID uuid = UUID.randomUUID();
        Map<String, Double> balances = new HashMap<>();
        balances.put("money", 10.0);

        cache.put(uuid, balances);

        Set<UUID> dirtyPlayers = cache.getAndClearDirtyPlayers();
        assertTrue(dirtyPlayers.contains(uuid), "Dirty set should contain newly cached player");

        Set<UUID> secondFetch = cache.getAndClearDirtyPlayers();
        assertTrue(secondFetch.isEmpty(), "Dirty set should be empty after clear");
    }

    @Test
    @DisplayName("InvalidateAll clears cached data and dirty tracking")
    void testInvalidateAllClearsCacheAndDirtyTracking() {
        Cache cache = new Cache();
        UUID uuid = UUID.randomUUID();
        Map<String, Double> balances = new HashMap<>();
        balances.put("money", 20.0);

        cache.put(uuid, balances);
        cache.invalidateAll();

        assertNull(cache.get(uuid), "Cache should be empty after invalidateAll");
        assertTrue(cache.getAndClearDirtyPlayers().isEmpty(), "Dirty set should be empty after invalidateAll");
    }
}
