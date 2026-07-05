package it.alzy.simpleeconomy.tests.commands;

import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.logging.TransactionLogger;
import it.alzy.simpleeconomy.plugin.model.DynamicCommand;
import it.alzy.simpleeconomy.plugin.model.VirtualCurrency;
import it.alzy.simpleeconomy.plugin.utils.DoubleUtils;
import it.alzy.simpleeconomy.tests.MockStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DynamicCommandTest {

    private ServerMock server;
    private SimpleEconomy plugin;
    private DynamicCommand command;
    private VirtualCurrency money;
    private ExecutorService testExecutor;

    @BeforeEach
    void setUp() throws Exception {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SimpleEconomy.class);

        // Forza l'impostazione del campo statico 'instance' per evitare NullPointerException
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

        Field transactionLoggerField = SimpleEconomy.class.getDeclaredField("transactionLogger");
        transactionLoggerField.setAccessible(true);
        transactionLoggerField.set(plugin, new TransactionLogger(plugin)); 
        testExecutor = Executors.newCachedThreadPool();
        try {
            Field executorField = SimpleEconomy.class.getDeclaredField("executor");
            executorField.setAccessible(true);
            executorField.set(plugin, testExecutor);
        } catch (NoSuchFieldException ignored) {
        }

        money = new VirtualCurrency("money", "$");
        command = new DynamicCommand(money);

        server.getScheduler().performOneTick();
    }

    @AfterEach
    void tearDown() {
        if (testExecutor != null && !testExecutor.isShutdown()) {
            testExecutor.shutdownNow();
        }
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Pay transfers balances between players")
    void testPayTransfersBalances() {
        PlayerMock sender = server.addPlayer();
        PlayerMock target = server.addPlayer();

        putBalance(sender.getUniqueId(), 100.0);
        putBalance(target.getUniqueId(), 25.0);

        command.onPay(sender, target.getName(), 12.34);

        server.getScheduler().performOneTick();

        assertEquals(87.66, money.getBalance(sender.getUniqueId()), 1e-9, "Sender balance should decrease by the payment amount");
        assertEquals(37.34, money.getBalance(target.getUniqueId()), 1e-9, "Target balance should increase by the payment amount");
    }

    @Test
    @DisplayName("Pay rejects amounts with more than two decimal places")
    void testPayRejectsHighPrecisionAmounts() {
        PlayerMock sender = server.addPlayer();
        PlayerMock target = server.addPlayer();

        putBalance(sender.getUniqueId(), 100.0);
        putBalance(target.getUniqueId(), 25.0);

        command.onPay(sender, target.getName(), 12.345);

        server.getScheduler().performOneTick();

        assertEquals(100.0, money.getBalance(sender.getUniqueId()), 1e-9, "Sender balance should remain unchanged when the amount is invalid");
        assertEquals(25.0, money.getBalance(target.getUniqueId()), 1e-9, "Target balance should remain unchanged when the amount is invalid");
    }

    @Test
    @DisplayName("Remove never drives a balance below zero")
    void testRemoveClampsAtZero() {
        PlayerMock sender = server.addPlayer();
        PlayerMock target = server.addPlayer();

        putBalance(target.getUniqueId(), 40.0);

        command.remove(sender, target.getName(), 75.0);

        server.getScheduler().performOneTick();

        assertEquals(0.0, money.getBalance(target.getUniqueId()), 1e-9, "Removed balances should not become negative");
    }

    @Test
    @DisplayName("Set applies the exact balance requested")
    void testSetAppliesExactBalance() {
        PlayerMock sender = server.addPlayer();
        PlayerMock target = server.addPlayer();

        putBalance(target.getUniqueId(), 40.0);

        command.set(sender, target.getName(), 99.99);

        server.getScheduler().performOneTick();

        assertEquals(99.99, money.getBalance(target.getUniqueId()), 1e-9, "Set should overwrite the current balance");
    }

    private void putBalance(UUID uuid, double amount) {
        Map<String, Double> balances = new ConcurrentHashMap<>();
        balances.put("money", amount);
        plugin.getCache().put(uuid, balances);
    }
}