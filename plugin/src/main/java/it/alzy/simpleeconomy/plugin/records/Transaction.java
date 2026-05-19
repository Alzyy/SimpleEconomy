package it.alzy.simpleeconomy.plugin.records;

import it.alzy.simpleeconomy.api.TransactionTypes;

public record Transaction(String uuid, String targetUUID, String currency, double amount, double balanceBefore, double balanceAfter, TransactionTypes type, long timestamp) {
}