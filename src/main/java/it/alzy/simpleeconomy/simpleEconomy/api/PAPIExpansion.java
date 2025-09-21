package it.alzy.simpleeconomy.simpleEconomy.api;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import it.alzy.simpleeconomy.simpleEconomy.SimpleEconomy;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class PAPIExpansion extends PlaceholderExpansion {

    private final SimpleEconomy plugin = SimpleEconomy.getInstance();

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
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        String lower = params.toLowerCase();

        if (lower.startsWith("baltop_")) {
            String numberPart = lower.substring("baltop_".length());
            boolean balanceRequest = numberPart.endsWith("_balance");
            if (balanceRequest) numberPart = numberPart.substring(0, numberPart.length() - "_balance".length());

            try {
                int position = Integer.parseInt(numberPart);
                Map<String, Double> snapshot = new HashMap<>(plugin.getTopMap());
                if (balanceRequest) {
                    return getPlayerBalanceAtPositionAsync(position, snapshot).join();
                } else {
                    return getPlayerNameAtPositionAsync(position, snapshot).join();
                }
            } catch (NumberFormatException e) {
                return "Invalid position!";
            }
        }

        switch (lower) {
            case "balance_formatted" -> {
                double balance = plugin.getCacheMap().getOrDefault(player.getUniqueId(), 0d);
                return plugin.getFormatUtils().formatBalance(balance);
            }
            case "balance_normal" -> {
                return String.valueOf(plugin.getCacheMap().getOrDefault(player.getUniqueId(), 0d));
            }
            case "top_position" -> {
                Map<String, Double> snapshot = new HashMap<>(plugin.getTopMap());
                return String.valueOf(getPlayerPositionAsync(player, snapshot).join());
            }
            default -> {
                return "Placeholder not found!";
            }
        }
    }


    private CompletableFuture<String> getPlayerNameAtPositionAsync(int position, Map<String, Double> topMap) {
        return CompletableFuture.supplyAsync(() -> {
            List<UUID> sorted = topMap.entrySet().stream()
                    .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                    .map(e -> UUID.fromString(e.getKey()))
                    .collect(Collectors.toList());

            if (position < 1 || position > sorted.size()) return "N/A";

            UUID uuid = sorted.get(position - 1);
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
            return (offline != null && offline.getName() != null) ? offline.getName() : "Unknown";
        });
    }

    private CompletableFuture<String> getPlayerBalanceAtPositionAsync(int position, Map<String, Double> topMap) {
        return CompletableFuture.supplyAsync(() -> {
            List<Double> sorted = topMap.entrySet().stream()
                    .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toList());

            if (position < 1 || position > sorted.size()) return "N/A";
            return plugin.getFormatUtils().formatBalance(sorted.get(position - 1));
        });
    }

    private CompletableFuture<Integer> getPlayerPositionAsync(Player player, Map<String, Double> topMap) {
        return CompletableFuture.supplyAsync(() -> {
            List<UUID> sorted = topMap.entrySet().stream()
                    .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                    .map(e -> UUID.fromString(e.getKey()))
                    .collect(Collectors.toList());

            int index = sorted.indexOf(player.getUniqueId());
            return (index == -1) ? -1 : index + 1;
        });
    }
}
