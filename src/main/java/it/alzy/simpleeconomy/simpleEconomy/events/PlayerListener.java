package it.alzy.simpleeconomy.simpleEconomy.events;

import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import it.alzy.simpleeconomy.simpleEconomy.SimpleEconomy;

public class PlayerListener implements Listener {
    private final SimpleEconomy plugin;

    public PlayerListener(SimpleEconomy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getPlayerProfile().getId();
        String name = event.getPlayerProfile().getName();

        plugin.getStorage().hasAccount(uuid).thenAccept(hasAccount -> {
            if(hasAccount) return;
            plugin.getStorage().create(uuid);
            plugin.getLogger().info("Created account for " + name + " (" + uuid + ")");
            return;
        });

        plugin.getStorage().load(uuid).thenAccept(balance -> {
            plugin.getLogger().info("Loaded balance for " + name + " (" + uuid + "): " + balance);
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Double balance = plugin.getCacheMap().get(uuid);

        if (balance != null) {
            plugin.getLogger().info("Saving economy data for: " + event.getPlayer().getName());
            plugin.getStorage().save(uuid, balance);
            plugin.getCacheMap().remove(uuid);
        } else {
            plugin.getLogger().warning("No cached balance to save for: " + event.getPlayer().getName());
        }
    }
}
