package it.alzy.simpleeconomy.tests.cache;

import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.storage.Cache;
import it.alzy.simpleeconomy.plugin.utils.DoubleUtils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

public class CacheTest {

    private SimpleEconomy plugin;

    @BeforeEach
    void setUp() throws Exception {
        ServerMock server = MockBukkit.mock();
        plugin = MockBukkit.load(SimpleEconomy.class);

        try {
            Field instanceField = SimpleEconomy.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, plugin);
        } catch (NoSuchFieldException e) {
        }

        Field doubleUtilsField = SimpleEconomy.class.getDeclaredField("doubleUtils");
        doubleUtilsField.setAccessible(true);
        doubleUtilsField.set(plugin, new DoubleUtils());

        server.getScheduler().performOneTick();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

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

    @Test
    @DisplayName("Test with Caffeine cache to ensure it behaves correctly under concurrent access")
    void testCaffeineCacheConcurrentAccess() throws InterruptedException {
        Cache cache = new Cache();
        UUID uuid = UUID.randomUUID();
        Map<String, Double> balances = new HashMap<>();
        balances.put("money", 30.0);

        cache.put(uuid, balances);

        Runnable updateTask = () -> {
            for (int i = 0; i < 100; i++) {
                cache.updateCurrency(uuid, "money", 30.0 + i);
            }
        };

        Thread thread1 = new Thread(updateTask);
        Thread thread2 = new Thread(updateTask);

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        assertNotNull(cache.get(uuid), "Cached balances should still be present after concurrent updates");
    }
}