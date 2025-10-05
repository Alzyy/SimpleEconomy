package it.alzy.simpleeconomy.api;

/**
 * Represents the result of a transaction in the SimpleEconomy system.
 * <p>
 * This enum is used as the return type for transfer operations to indicate
 * whether the transaction was successful or if an error occurred.
 * </p>
 *
 * <ul>
 *     <li>{@link #SUCCESS} – The transaction completed successfully.</li>
 *     <li>{@link #INSUFFICIENT_FUNDS} – The transaction failed because the user
 *         did not have enough balance.</li>
 *     <li>{@link #ERROR} – The transaction could not be completed due to an
 *         unexpected error (e.g., database failure).</li>
 * </ul>
 */
public enum TransactionResult {
    /**
     * Transaction completed successfully.
     */
    SUCCESS,

    /**
     * Transaction failed due to insufficient funds in the user's account.
     */
    INSUFFICIENT_FUNDS,

    /**
     * Transaction failed due to an unexpected error (e.g., database issue).
     */
    ERROR,
}
