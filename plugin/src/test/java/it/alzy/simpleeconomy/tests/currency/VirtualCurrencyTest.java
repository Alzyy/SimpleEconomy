package it.alzy.simpleeconomy.tests.currency;

import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.managers.CurrencyManager;
import it.alzy.simpleeconomy.plugin.model.VirtualCurrency;
import it.alzy.simpleeconomy.tests.MockStorage;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

public class VirtualCurrencyTest {
    private SimpleEconomy plugin;

    @BeforeEach
    void setUp() throws Exception {
        ServerMock server = MockBukkit.mock();
        plugin = MockBukkit.load(SimpleEconomy.class);

        Field storageField = SimpleEconomy.class.getDeclaredField("storage");
        storageField.setAccessible(true);
        storageField.set(plugin, new MockStorage());

        Field managerField = SimpleEconomy.class.getDeclaredField("currencyManager");
        managerField.setAccessible(true);
        managerField.set(plugin, new CurrencyManager());
        
        server.getScheduler().performOneTick();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Checks currency separation between main balance and virtual currency")
    void testCurrencySeparation() {
        assertNotNull(plugin.getStorage(), "Storage should be correctly mocked");
        UUID uuid = UUID.randomUUID();
        
        Map<String, Double> balances = new ConcurrentHashMap<>();
        balances.put("money", 100.0);
        
        plugin.getCache().put(uuid, balances);
        
        VirtualCurrency crystals = plugin.getCurrencyManager().getCurrency("crystals");
        if (crystals != null) {
            crystals.setBalance(uuid, 50.0);
            
            Map<String, Double> cachedBalances = plugin.getCache().get(uuid);
            assertNotNull(cachedBalances, "Player balances map should not be null in cache");
            
            double mainBal = cachedBalances.getOrDefault("money", 0.0);
            double crystalBal = crystals.getBalance(uuid);
            
            assertNotEquals(mainBal, crystalBal, "The currencies must be separate");
            assertEquals(100.0, mainBal, "Main balance ('money') should remain 100.0");
            assertEquals(50.0, crystalBal, "Virtual currency balance ('crystals') should be 50.0");
        }
    }

    @Test
    @DisplayName("Normalizes currency keys into storage-safe format")
    void testColumnOrKeyNormalization() {
        VirtualCurrency currency = new VirtualCurrency("Crystal Coins", "CC");
        assertEquals("crystal_coins", currency.getColumnOrKey(), "Currency keys should be normalized to snake_case");
    }

}