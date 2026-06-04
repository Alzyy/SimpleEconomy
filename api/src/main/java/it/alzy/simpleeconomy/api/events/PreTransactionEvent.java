package it.alzy.simpleeconomy.api.events;

import java.util.UUID;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import it.alzy.simpleeconomy.api.TransactionTypes;

public class PreTransactionEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final UUID sender;
    private final UUID receiver;
    private final String currency;
    private final double amount;
    private final TransactionTypes type;
    private boolean cancelled = false;
    private String cancelMessage = "Transaction cancelled.";

    public PreTransactionEvent(UUID sender, UUID receiver, String currency, double amount, TransactionTypes type) {
        super(true);
        this.sender = sender;
        this.receiver = receiver;
        this.currency = currency;
        this.amount = amount;
        this.type = type;
    }

    public UUID getSender() {
        return sender;
    }

    public UUID getReceiver() {
        return receiver;
    }
    public double getAmount() {
        return amount;
    }

    public TransactionTypes getType() {
        return type;
    }
    
    public String getCurrency() {
        return currency;
    }

    public String getCancelMessage() {
        return cancelMessage;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    public String getCancelReason() {
        return cancelMessage;
    }
    public void setCancelReason(String reason) {
        this.cancelMessage = reason;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }


    
}
