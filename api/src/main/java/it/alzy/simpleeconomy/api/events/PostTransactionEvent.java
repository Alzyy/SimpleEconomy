package it.alzy.simpleeconomy.api.events;

import it.alzy.simpleeconomy.api.TransactionTypes;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import java.util.UUID;

public class PostTransactionEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    
    private final UUID sender;
    private final UUID receiver;
    private final double amount;
    private final String currency;
    private final TransactionTypes type;

    public PostTransactionEvent(UUID sender, UUID receiver, String currency, double amount, TransactionTypes type) { 
        super(true); 
        this.sender = sender;
        this.receiver = receiver;
        this.currency = currency;
        this.amount = amount;
        this.type = type;
    }

    public UUID getSender() { return sender; }
    public UUID getReceiver() { return receiver; }
    public double getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public TransactionTypes getType() { return type; }

    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}