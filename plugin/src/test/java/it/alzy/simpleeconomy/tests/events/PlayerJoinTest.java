package it.alzy.simpleeconomy.tests.events;

import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.configurations.SettingsConfig;
import it.alzy.simpleeconomy.plugin.utils.DoubleUtils;
import it.alzy.simpleeconomy.tests.MockStorage;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class PlayerJoinTest {
    private ServerMock server;
    private SimpleEconomy plugin;

    @BeforeEach
    void setUp() throws Exception {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SimpleEconomy.class);

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
    @DisplayName("Checks if player balance is loaded on join")
    void testInitialBalanceLoading() {
        PlayerMock player = server.addPlayer();

        Map<String, Double> balances = plugin.getStorage().load(player.getUniqueId()).join();
        
        assertNotNull(balances, "Balances map should not be null after loading");
        assertFalse(balances.isEmpty(), "Balances map should not be empty for a new player");

        double expectedStartingBalance = SettingsConfig.getInstance().startingBalance();
        assertTrue(balances.containsKey("money"), "Balances map must contain the default 'money' currency");
        assertEquals(expectedStartingBalance, balances.get("money"), "The loaded balance does not match the starting balance");
    }
}