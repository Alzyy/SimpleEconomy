package it.alzy.simpleeconomy.plugin.api;

import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.configurations.SettingsConfig;
import it.alzy.simpleeconomy.plugin.records.TopEntry;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class PAPIExpansion extends PlaceholderExpansion {

    private final SimpleEconomy plugin = SimpleEconomy.getInstance();
    
    private final AtomicReference<List<TopEntry>> topCache = new AtomicReference<>(new ArrayList<>());
    private long lastUpdate = 0;
    private final long CACHE_TIME = 30000; // 30 seconds

    @Override
    public @NotNull String getIdentifier() {
        return "seco";
    }

    @Override
    public @NotNull String getAuthor() {
        return "AlzyIT";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        String lower = params.toLowerCase();

        if (lower.startsWith("baltop_")) {
            updateTopCache(); 
            
            String numberPart = lower.substring(7);
            boolean isBalanceRequest = numberPart.endsWith("_balance");
            if (isBalanceRequest) {
                numberPart = numberPart.substring(0, numberPart.length() - 8);
            }

            try {
                int position = Integer.parseInt(numberPart);
                List<TopEntry> currentTop = topCache.get();

                if (position < 1 || position > currentTop.size()) return "N/A";
                
                TopEntry entry = currentTop.get(position - 1);
                return isBalanceRequest ? plugin.getFormatUtils().formatBalance(entry.balance()) : entry.name();
                
            } catch (NumberFormatException e) {
                return "Invalid position!";
            }
        }

        if (lower.startsWith("currency_")) {
            String currencyName = params.substring(9).toLowerCase();
            
            Map<String, Double> balances = plugin.getCache().get(player.getUniqueId());
            double bal = (balances != null && balances.containsKey(currencyName)) 
                    ? balances.get(currencyName) 
                    : 0d; 

            var currencyInfo = plugin.getCurrencyManager() != null ? plugin.getCurrencyManager().getCurrency(currencyName) : null;
            if (currencyInfo != null) {
                return plugin.getFormatUtils().formatVirtualCurrencyBalance(currencyInfo, bal);
            }
            
            return plugin.getFormatUtils().formatBalance(bal);
        }

        return switch (lower) {
            case "balance_formatted" -> {
                double balance = getCachedBalance(player.getUniqueId(), "money");
                yield plugin.getFormatUtils().formatBalance(balance);
            }
            case "balance_normal" -> {
                double balance = getCachedBalance(player.getUniqueId(), "money");
                yield String.valueOf(balance);
            }
            case "top_position" -> {
                updateTopCache();
                int pos = getPlayerPosition(player.getUniqueId());
                yield pos > 0 ? String.valueOf(pos) : "N/A";
            }
            default -> null;
        };
    }

    private double getCachedBalance(UUID uuid, String currency) {
        Map<String, Double> balances = plugin.getCache().get(uuid);
        if (balances != null && balances.containsKey(currency)) {
            return balances.get(currency);
        }
        return "money".equals(currency) ? SettingsConfig.getInstance().startingBalance() : 0d;
    }

    private void updateTopCache() {
        long now = System.currentTimeMillis();
        if (now - lastUpdate < CACHE_TIME) return;
        lastUpdate = now;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<String, Double> topFromDB = plugin.getStorage().getTopBalances("money", 100);
            
            List<TopEntry> updatedTop = new ArrayList<>();
            for (Map.Entry<String, Double> entry : topFromDB.entrySet()) {
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    String name = org.bukkit.Bukkit.getOfflinePlayer(uuid).getName();
                    updatedTop.add(new TopEntry(name != null ? name : "Unknown", entry.getValue(), uuid));
                } catch (IllegalArgumentException ignored) {
                }
            }
            
            topCache.set(updatedTop);
        });
    }

    private int getPlayerPosition(UUID uuid) {
        List<TopEntry> currentTop = topCache.get();
        for (int i = 0; i < currentTop.size(); i++) {
            if (currentTop.get(i).uuid().equals(uuid)) return i + 1;
        }
        return -1;
    }
}