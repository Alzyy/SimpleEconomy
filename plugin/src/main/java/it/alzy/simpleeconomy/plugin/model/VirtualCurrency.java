package it.alzy.simpleeconomy.plugin.model;

import java.util.Map;
import java.util.UUID;

import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import lombok.Data;
import lombok.Setter;

@Data
public class VirtualCurrency {
    
    private final String name;
    
    @Setter
    private String symbol;
    
    private final String columnOrKey;

    public VirtualCurrency(String name, String symbol) {
        this.name = name;
        this.symbol = symbol;
        this.columnOrKey = name.toLowerCase().replaceAll("\\s+", "_");
    }

    public double getBalance(UUID playerId) {
        Map<String, Double> balances = SimpleEconomy.getInstance().getCache().get(playerId);
        
        if (balances != null && balances.containsKey(this.columnOrKey)) {
            return balances.get(this.columnOrKey);
        }
        
        return 0.0;
    }

    public void setBalance(UUID playerId, double amount) {
        SimpleEconomy plugin = SimpleEconomy.getInstance();

        if (plugin.getCache().contains(playerId)) {
            plugin.getCache().updateCurrency(playerId, this.columnOrKey, amount);
        }

        plugin.getExecutor().execute(() -> plugin.getStorage().saveSync(playerId, this.columnOrKey, amount));
    }
}