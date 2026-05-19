package it.alzy.simpleeconomy.api;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interface representing an economy provider for the SimpleEconomy plugin.
 *
 * <p>Implementations of this interface handle all operations related to
 * player balances, account management, and money transfers across multiple currencies.
 * All methods are asynchronous and return {@link CompletableFuture} to
 * support non-blocking operations.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * EconomyProvider provider = SimpleEconomyAPI.getProvider();
 * UUID playerId = player.getUniqueId();
 * String currency = "money";
 * provider.getBalance(playerId, currency).thenAccept(balance -> {
 *     System.out.println("Player balance: " + balance);
 * });
 * }</pre>
 */
public interface EconomyProvider {

    /**
     * Retrieves the current balance of a player for a specific currency.
     *
     * @param uuid the UUID of the player
     * @param currency the name of the currency to check
     * @return a {@link CompletableFuture} containing the player's balance as a {@link Double}
     */
    CompletableFuture<Double> getBalance(UUID uuid, String currency);

    /**
     * Sets the balance of a player to a specific amount for a given currency.
     *
     * @param uuid the UUID of the player
     * @param currency the name of the currency to modify
     * @param amount the amount to set as the player's balance
     * @return a {@link CompletableFuture} that completes when the operation is done
     */
    CompletableFuture<Void> setBalance(UUID uuid, String currency, double amount);

    /**
     * Deposits a specific amount of money into a player's account for a given currency.
     *
     * @param uuid the UUID of the player
     * @param currency the name of the currency to deposit
     * @param amount the amount to deposit
     * @return a {@link CompletableFuture} that completes when the operation is done
     */
    CompletableFuture<Void> deposit(UUID uuid, String currency, double amount);

    /**
     * Deducts a specific amount of money from a player's account for a given currency.
     *
     * @param uuid the UUID of the player
     * @param currency the name of the currency to deduct from
     * @param amount the amount to deduct
     * @return a {@link CompletableFuture} containing {@code true} if the operation succeeded,
     *         or {@code false} if the player does not have enough funds
     */
    CompletableFuture<Boolean> detract(UUID uuid, String currency, double amount);

    /**
     * Checks if a player has an economy account registered in the system.
     *
     * @param uuid the UUID of the player
     * @return a {@link CompletableFuture} containing {@code true} if the player has an account,
     *         or {@code false} otherwise
     */
    CompletableFuture<Boolean> hasAccount(UUID uuid);

    /**
     * Checks if a player has enough balance to cover a specific amount in a given currency.
     *
     * @param uuid the UUID of the player
     * @param currency the name of the currency to check
     * @param amount the amount to check against the balance
     * @return a {@link CompletableFuture} containing {@code true} if the player has enough balance,
     *         or {@code false} otherwise
     */
    CompletableFuture<Boolean> hasEnough(UUID uuid, String currency, double amount);

    /**
     * Transfers a specific amount of money from one player to another in a given currency.
     *
     * @param from the UUID of the player sending the money
     * @param to the UUID of the player receiving the money
     * @param currency the name of the currency being transferred
     * @param amount the amount to transfer
     * @return a {@link CompletableFuture} containing a {@link TransactionResult} indicating
     *         the outcome of the transfer
     */
    CompletableFuture<TransactionResult> transfer(UUID from, UUID to, String currency, double amount);


    /**
     * Retrieves the set of all available virtual currencies in the economy.
     *
     * @return a {@link Set} of currency names
     */
    Set<String> getAvailableVirtualCurrencies();
    
}