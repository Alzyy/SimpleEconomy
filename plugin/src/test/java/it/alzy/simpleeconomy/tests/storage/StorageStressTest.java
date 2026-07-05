package it.alzy.simpleeconomy.tests.storage;

import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.utils.DoubleUtils;
import it.alzy.simpleeconomy.tests.MockStorage;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class StorageStressTest {
    
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

        Field storageField = SimpleEconomy.class.getDeclaredField("storage");
        storageField.setAccessible(true);
        storageField.set(plugin, new MockStorage());
        
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
    @DisplayName("Stress test for bulk saving performance with multiple currencies and dirty tracking")
    void testBulkSavePerformance() {
        assertNotNull(plugin.getStorage(), "Storage should be correctly mocked");

        for (int i = 0; i < 1000; i++) {
            UUID uuid = UUID.randomUUID();
            
            Map<String, Double> balances = new ConcurrentHashMap<>();
            balances.put("money", (double) i);
            balances.put("crystals", (double) (i * 2));
            
            plugin.getCache().put(uuid, balances);
        }

        plugin.getStorage().bulkSave();
    }
}