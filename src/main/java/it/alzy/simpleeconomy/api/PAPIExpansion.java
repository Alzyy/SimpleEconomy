package it.alzy.simpleeconomy.api;


import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import it.alzy.simpleeconomy.simpleEconomy.SimpleEconomy;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class PAPIExpansion extends PlaceholderExpansion{

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
        return(SimpleEconomy.getInstance().getPluginMeta().getVersion());
    }
    



    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        switch(params.toLowerCase()) {
            case "balance_formatted" -> {
                double balance = plugin.getCacheMap().getOrDefault(player.getUniqueId(), 0d);
                return plugin.getFormatUtils().formatBalance(balance);
            }
            case "balance_normal" -> {
                return String.valueOf(plugin.getCacheMap().getOrDefault(player.getUniqueId(), 0d));
            }
            default -> {
                return "Placeholder not found!";
            }
        }
    }
}
