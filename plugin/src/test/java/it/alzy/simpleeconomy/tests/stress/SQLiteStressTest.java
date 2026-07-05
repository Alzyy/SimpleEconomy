package it.alzy.simpleeconomy.tests.stress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;

import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.storage.impl.SQLiteStorage;
import it.alzy.simpleeconomy.plugin.utils.DoubleUtils;

public class SQLiteStressTest {
    private SimpleEconomy plugin;
    private SQLiteStorage sqlite;
    
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        MockBukkit.mock();
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

        sqlite = new SQLiteStorage(plugin);
        Field storageField = SimpleEconomy.class.getDeclaredField("storage");
        storageField.setAccessible(true);
        storageField.set(plugin, sqlite);
        sqlite.init();

    }

    @AfterEach
    void tearDown() {
        sqlite.close();
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Stress test for SQLite storage with concurrent operations")
    void testBulkSQLiteStress() throws Exception {
        final int PLAYER_COUNT = 10_000;
               for (int i = 0; i < PLAYER_COUNT; i++) {
            UUID uuid = UUID.randomUUID();
            Map<String, Double> balances = new ConcurrentHashMap<>();
            balances.put("money", Math.random() * 1000);
            balances.put("crystals", Math.random() * 100);
            plugin.getCache().put(uuid, balances);
        }

        long start = System.nanoTime();
        plugin.getStorage().bulkSave();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        System.out.printf("Bulk save time: %d ms%n", elapsedMs);

        assertTrue(elapsedMs < 5000, "Bulk save took too long, expected less than 5 seconds");
    }

    @Test
    @DisplayName("Stress test for SQLite storage with concurrent load operations")
    void testBulkSQLiteLoadStress() throws Exception {
        UUID uuid = UUID.randomUUID();
        sqlite.create(uuid);

        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            sqlite.saveSync(uuid, "money", 1000.0 + i);
            double bal = sqlite.getBalance(uuid, "money").join();
            assertEquals(1000.0 + i, bal, 0.0001);
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;


        System.out.printf("1000 save and load operations took %d ms%n", elapsedMs);
    }

}
