package it.alzy.simpleeconomy.plugin.events;

import java.util.UUID;

import it.alzy.simpleeconomy.plugin.configurations.LangConfig;
import it.alzy.simpleeconomy.plugin.utils.ChatUtils;
import it.alzy.simpleeconomy.plugin.utils.UpdateUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import it.alzy.simpleeconomy.plugin.SimpleEconomy;

public class PlayerListener implements Listener {
    private final SimpleEconomy plugin;

    public PlayerListener(SimpleEconomy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerLogin(AsyncPlayerPreLoginEvent event) {
        if(!event.getLoginResult().equals(AsyncPlayerPreLoginEvent.Result.ALLOWED)) return;

        UUID uuid = event.getUniqueId();
        String name = event.getName();

        plugin.getStorage().hasAccount(uuid).thenAccept(hasAccount -> {
            if(hasAccount) return;
            plugin.getStorage().create(uuid);
            plugin.getLogger().info("Created account for " + name + " (" + uuid + ")");
        });

        plugin.getStorage().load(uuid).thenAccept(balance -> {
            plugin.getLogger().info("Loaded balance for " + name + " (" + uuid + "): " + balance);
        });
    }


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if(player.hasPermission("simpleeconomy.notify.update")) {
            plugin.getExecutor().submit(() -> {
                if(UpdateUtils.isUpdateAvailable()) {
                    ChatUtils.send(player, LangConfig.getInstance().UPDATES_AVAILABLE,
                            "%prefix%", LangConfig.getInstance().PREFIX,
                            "%version%", UpdateUtils.getNewVersion(),
                            "%updateNote%", UpdateUtils.getUpdateNotes());
                } else {
                    ChatUtils.send(player, LangConfig.getInstance().UPDATES_LATEST,
                            "%prefix%", LangConfig.getInstance().PREFIX);
                }
            });
        }
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
