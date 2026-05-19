package it.alzy.simpleeconomy.plugin.records;

import java.util.UUID;

public record TopEntry(String name, double balance, UUID uuid) {
    public TopEntry(String name, double balance, UUID uuid) {
        this.name = name;
        this.balance = balance;
        this.uuid = uuid;
    }

    public String name() {
        return name;
    }
    public double balance() {
        return balance;
    }
    public UUID uuid() {
        return uuid;
    }
}
