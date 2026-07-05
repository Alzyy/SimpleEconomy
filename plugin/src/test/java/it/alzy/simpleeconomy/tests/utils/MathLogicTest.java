package it.alzy.simpleeconomy.tests.utils;

import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.utils.DoubleUtils;
import it.alzy.simpleeconomy.plugin.utils.TransactionHelper;
import it.alzy.simpleeconomy.tests.MockStorage;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MathLogicTest {

    private ServerMock server;
    private SimpleEconomy plugin;
    private DoubleUtils doubleUtils;
    private TransactionHelper transactionHelper;
    private CommandSender sender;

    @BeforeEach
    void setUp() throws Exception {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SimpleEconomy.class);

        try {
            Field instanceField = SimpleEconomy.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, plugin);
        } catch (NoSuchFieldException ignored) {
        }

        Field storageField = SimpleEconomy.class.getDeclaredField("storage");
        storageField.setAccessible(true);
        storageField.set(plugin, new MockStorage());

        Field doubleUtilsField = SimpleEconomy.class.getDeclaredField("doubleUtils");
        doubleUtilsField.setAccessible(true);
        doubleUtilsField.set(plugin, new DoubleUtils());

        doubleUtils = plugin.getDoubleUtils();
        transactionHelper = plugin.getTransactionHelper();
        sender = server.getConsoleSender();

        server.getScheduler().performOneTick();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Rounds balances to the configured decimal places")
    void testDoubleUtilsRoundsToTwoDecimals() {
        assertEquals(10.24, doubleUtils.round(10.235), 1e-9, "Half-up rounding should be applied at the third decimal place");
        assertEquals(10.23, doubleUtils.round(10.234), 1e-9, "Values below the midpoint should round down");
    }

    @Test
    @DisplayName("Valid amounts are accepted and high precision or invalid values are rejected")
    void testTransactionHelperAmountValidation() {
        assertFalse(transactionHelper.isNotValidAmount(sender, 12.34), "Two-decimal amounts should be accepted");
        assertTrue(transactionHelper.isNotValidAmount(sender, 12.345), "Amounts with more than two decimals should be rejected");
        assertTrue(transactionHelper.isNotValidAmount(sender, Double.NaN), "NaN should be rejected");
    }
}