package it.alzy.simpleeconomy.api;

/**
 * Categorizes transaction operations for audit/logging purposes.
 */
public enum TransactionTypes {
    /**
     * Player-to-player payment.
     */
    PAY("pay"),
    /**
     * Player withdrawal.
     */
    WITHDRAW("withdraw"),
    /**
     * Player deposit.
     */
    DEPOSIT("deposit"),
    /**
     * Administrative balance adjustment.
     */
    ADMIN_ADJUSTMENT("admin_adjustment");

    private final String type;

    TransactionTypes(String type) {
        this.type = type;
    }

    /**
     * Returns the serialized identifier for this transaction type.
     *
     * @return the transaction type identifier
     */
    public String type() {
        return type;
    }
}
