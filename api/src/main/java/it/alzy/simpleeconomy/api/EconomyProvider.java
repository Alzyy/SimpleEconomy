package it.alzy.simpleeconomy.api;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interface representing an economy provider for the SimpleEconomy plugin.
 *
 * <p>Implementations of this interface handle all operations related to
 * player balances, account management, and money transfers.
 * All methods are asynchronous and return {@link CompletableFuture} to
 * support non-blocking operations.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * EconomyProvider provider = SimpleEconomyAPI.getProvider();
 * UUID playerId = player.getUniqueId();
 * provider.getBalance(playerId).thenAccept(balance -> {
 *     System.out.println("Player balance: " + balance);
 * });
 * }</pre>
 */
public interface EconomyProvider {

    /**
     * Retrieves the current balance of a player.
     *
     * @param uuid the UUID of the player
     * @return a {@link CompletableFuture} containing the player's balance as a {@link Double}
     */
    CompletableFuture<Double> getBalance(UUID uuid);

    /**
     * Sets the balance of a player to a specific amount.
     *
     * @param uuid the UUID of the player
     * @param amount the amount to set as the player's balance
     * @return a {@link CompletableFuture} that completes when the operation is done
     */
    CompletableFuture<Void> setBalance(UUID uuid, double amount);

    /**
     * Deposits a specific amount of money into a player's account.
     *
     * @param uuid the UUID of the player
     * @param amount the amount to deposit
     * @return a {@link CompletableFuture} that completes when the operation is done
     */
    CompletableFuture<Void> deposit(UUID uuid, double amount);

    /**
     * Deducts a specific amount of money from a player's account.
     *
     * @param uuid the UUID of the player
     * @param amount the amount to deduct
     * @return a {@link CompletableFuture} containing {@code true} if the operation succeeded,
     *         or {@code false} if the player does not have enough funds
     */
    CompletableFuture<Boolean> detract(UUID uuid, double amount);

    /**
     * Checks if a player has an economy account.
     *
     * @param uuid the UUID of the player
     * @return a {@link CompletableFuture} containing {@code true} if the player has an account,
     *         or {@code false} otherwise
     */
    CompletableFuture<Boolean> hasAccount(UUID uuid);

    /**
     * Checks if a player has enough balance to cover a specific amount.
     *
     * @param uuid the UUID of the player
     * @param amount the amount to check
     * @return a {@link CompletableFuture} containing {@code true} if the player has enough balance,
     *         or {@code false} otherwise
     */
    CompletableFuture<Boolean> hasEnough(UUID uuid, double amount);

    /**
     * Transfers a specific amount of money from one player to another.
     *
     * @param from the UUID of the player sending the money
     * @param to the UUID of the player receiving the money
     * @param amount the amount to transfer
     * @return a {@link CompletableFuture} containing a {@link TransactionResult} indicating
     *         the outcome of the transfer
     */
    CompletableFuture<TransactionResult> transfer(UUID from, UUID to, double amount);
}