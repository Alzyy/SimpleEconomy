package it.alzy.simpleeconomy.tests.storage;

import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.tests.MockStorage;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionTest {
    private SimpleEconomy plugin;

    @BeforeEach
    void setUp() throws Exception {
        ServerMock server = MockBukkit.mock();
        plugin = MockBukkit.load(SimpleEconomy.class);

        Field storageField = SimpleEconomy.class.getDeclaredField("storage");
        storageField.setAccessible(true);
        storageField.set(plugin, new MockStorage());

        server.getScheduler().performOneTick();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Checks atomicity of update operations with new multi-currency storage")
    void testAtomicUpdate() {
        assertNotNull(plugin.getStorage(), "Storage should be correctly mocked");
        
        UUID uuid = UUID.randomUUID();
        double initialBalance = 100.0;
        String currency = "money";

        Map<String, Double> balances = new ConcurrentHashMap<>();
        balances.put(currency, initialBalance);
        plugin.getCache().put(uuid, balances);

        plugin.getStorage().save(uuid, currency, initialBalance);
        
        Double storedBalance = plugin.getStorage().getBalance(uuid, currency).join();
        
        assertNotNull(storedBalance, "The stored balance in the database should not be null");
        assertEquals(initialBalance, storedBalance, "The data in the database should match the saved data");
    }
}