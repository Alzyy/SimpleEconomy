package it.alzy.simpleeconomy.plugin.events;

import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.i18n.LanguageManager;
import it.alzy.simpleeconomy.plugin.i18n.enums.LanguageKeys;
import it.alzy.simpleeconomy.plugin.utils.UpdateUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;

public record PlayerListener(SimpleEconomy plugin, LanguageManager languageManager) implements Listener {

    @EventHandler
    public void onPlayerLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;

        UUID uuid = event.getUniqueId();

        plugin.getStorage().load(uuid).exceptionally(e -> {
            plugin.getLogger().severe("Failed to load economy data for " + event.getName() + ": " + e.getMessage());
            return null;
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPermission("simpleeconomy.notify.update")) return;

        if (UpdateUtils.isUpdateAvailable()) {
            languageManager.send(player, LanguageKeys.UPDATE_AVAILABLE, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX), "%version%", UpdateUtils.getNewVersion());
        } else {
            languageManager.send(player, LanguageKeys.UPDATE_LATEST, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        Map<String, Double> balances = plugin.getCache().get(uuid);

        if (balances != null) {
            plugin.getExecutor().execute(() -> {
                for (Map.Entry<String, Double> entry : balances.entrySet()) {
                    plugin.getStorage().save(uuid, entry.getKey(), entry.getValue());
                }

                plugin.getCache().remove(uuid);
            });
        }
    }
}