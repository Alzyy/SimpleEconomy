package it.alzy.simpleeconomy.tests.stress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.managers.CurrencyManager;
import it.alzy.simpleeconomy.plugin.model.VirtualCurrency;
import it.alzy.simpleeconomy.plugin.storage.Cache;
import it.alzy.simpleeconomy.tests.MockStorage;

public class LargeScaleStressTest {

    private SimpleEconomy plugin;

    @BeforeEach
    void setUp() throws Exception {
        MockBukkit.mock();
        plugin = MockBukkit.load(SimpleEconomy.class);


        Field instanceField = SimpleEconomy.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, plugin);

        Field executorField = SimpleEconomy.class.getDeclaredField("executor");
        executorField.setAccessible(true);
        executorField.set(plugin, Executors.newFixedThreadPool(2));

        Field cacheField = SimpleEconomy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        Cache cache = new Cache();
        cacheField.set(plugin, cache);

        Field storageField = SimpleEconomy.class.getDeclaredField("storage");
        storageField.setAccessible(true);
        storageField.set(plugin, new MockStorage());

        CurrencyManager currencyManager = new CurrencyManager();

        Field currencyField = CurrencyManager.class.getDeclaredField("currencies");
        currencyField.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, VirtualCurrency> currencies = (Map<String, VirtualCurrency>) currencyField.get(currencyManager);
        currencies.put("crystals", new VirtualCurrency("crystals", "C"));

        Field managerField = SimpleEconomy.class.getDeclaredField("currencyManager");
        managerField.setAccessible(true);
        managerField.set(plugin, currencyManager);


    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Simulation of 10k players with concurrent cache/storage operations")
    void simulate() throws InterruptedException {
        final int PLAYER_COUNT = 10_000;
        final int OPS_PER_PLAYER = 50;
        final int THREADS = 1;

        List<UUID> players = new ArrayList<>(PLAYER_COUNT);
        for(int i = 0; i < PLAYER_COUNT; i++) {
            UUID uuid = UUID.randomUUID();
            players.add(uuid);
            Map<String, Double> balances = new ConcurrentHashMap<>();
            balances.put("money", 1000.0);
            plugin.getCache().put(uuid, balances);
        }

        VirtualCurrency vc = plugin.getCurrencyManager().getCurrency("crystals");
        assertNotNull(vc, "Virtual currency 'crystals' should be initialized");
        
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        AtomicInteger interruptedCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(PLAYER_COUNT);
        AtomicReference<Throwable> firstError = new AtomicReference<>();

        long start = System.nanoTime();

        for(UUID uuid : players) {
            executor.submit(() -> {
                try {
                    for(int j = 0; j < OPS_PER_PLAYER; j++) {
                        double d = (j % 2 == 0) ? 1.5 : -0.5;
                        vc.setBalance(uuid, vc.getBalance(uuid) + d);

                        Map<String, Double> bal = plugin.getCache().get(uuid);
                        if(bal == null) {
                            interruptedCount.incrementAndGet();
                            continue;
                        }
                        bal.put("money", bal.getOrDefault("money", 0.0) + d);
                    }
                } catch (Throwable e) {
                    firstError.set(e);
                    interruptedCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        executor.shutdownNow();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertTrue(completed, "The stress test did not complete in time");
        assertEquals(0, interruptedCount.get(), "There were interruptions during the stress test");
        assertNull(firstError.get(), "There were errors during the stress test");

        double expectedDelta = (OPS_PER_PLAYER / 2) * 1.5 - (OPS_PER_PLAYER / 2) * 0.5;
        int mismatches = 0;
        for (UUID uuid : players) {
            double money = plugin.getCache().get(uuid).getOrDefault("money", 0.0);
            double crystalBal = vc.getBalance(uuid);
            if (Math.abs(money - (1000.0 + expectedDelta)) > 0.0001) mismatches++;
            if (Math.abs(crystalBal - expectedDelta) > 0.0001) mismatches++;
        }

        System.out.printf("[StressTest] %d player, %d total operations, completed in %d ms, mismatches=%d%n", PLAYER_COUNT, PLAYER_COUNT * OPS_PER_PLAYER, elapsedMs, mismatches);
        assertEquals(0, mismatches, "There were mismatches in the stress test (maybe race conditions)");
    }
}