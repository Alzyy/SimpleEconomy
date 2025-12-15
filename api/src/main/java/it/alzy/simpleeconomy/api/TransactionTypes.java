package it.alzy.simpleeconomy.api;

public enum TransactionTypes {
    PAY("pay"),
    WITHDRAW("withdraw"),
    DEPOSIT("deposit"),
    ADMIN_ADJUSTMENT("admin_adjustment");

    private final String type;

    TransactionTypes(String type) {
        this.type = type;
    }

    public String type() {
        return type;
    }
}
