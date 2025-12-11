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

import java.util.UUID;

public class PlayerListener implements Listener {

    private final SimpleEconomy plugin;
    private final LanguageManager languageManager;

    public PlayerListener() {
        this.plugin = SimpleEconomy.getInstance();
        this.languageManager = plugin.getLanguageManager();
    }

    @EventHandler
    public void onPlayerLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;

        UUID uuid = event.getUniqueId();

        try {
            if (!plugin.getStorage().hasAccount(uuid).join()) {
                plugin.getStorage().create(uuid);
            }

            Double balance = plugin.getStorage().load(uuid).join();
            plugin.getCacheMap().put(uuid, balance != null ? balance : 0.0);

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load economy data for " + event.getName() + ": " + e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPermission("simpleeconomy.notify.update")) return;

        if (UpdateUtils.isUpdateAvailable()) {
            languageManager.send(player, LanguageKeys.UPDATE_AVAILABLE, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX), "%version%", UpdateUtils.getNewVersion(), "%updateNote%", UpdateUtils.getUpdateNotes());
        } else {
            languageManager.send(player, LanguageKeys.UPDATE_LATEST, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Double balance = plugin.getCacheMap().remove(uuid);

        if (balance != null) {
            plugin.getExecutor().execute(() -> plugin.getStorage().save(uuid, balance));
        }
    }
}