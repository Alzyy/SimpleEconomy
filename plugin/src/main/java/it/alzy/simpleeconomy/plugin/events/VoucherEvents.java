package it.alzy.simpleeconomy.plugin.events;

import it.alzy.simpleeconomy.api.TransactionTypes;
import it.alzy.simpleeconomy.plugin.i18n.LanguageManager;
import it.alzy.simpleeconomy.plugin.i18n.enums.LanguageKeys;
import it.alzy.simpleeconomy.plugin.records.Transaction;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot; // Import this
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.configurations.SettingsConfig;
import it.alzy.simpleeconomy.plugin.utils.VaultHook;

public class VoucherEvents implements Listener {

    private final SimpleEconomy plugin = SimpleEconomy.getInstance();
    private final LanguageManager languageManager = plugin.getLanguageManager();

    @EventHandler
    public void playerInteract(PlayerInteractEvent ev) {
        if (ev.getHand() == null) return;

        Player player = ev.getPlayer();
        ItemStack playerItem = ev.getItem();

        if (playerItem == null) return;

        Material voucherMaterial = Material.getMaterial(SettingsConfig.getInstance().getVoucherMaterial());
        if (voucherMaterial == null) return;

        if (playerItem.getType() != voucherMaterial) return;
        if (!playerItem.hasItemMeta()) return;

        ItemMeta meta = playerItem.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (!pdc.has(plugin.getUuidKey(), PersistentDataType.STRING)) return;
        if (!pdc.has(plugin.getAmountKey(), PersistentDataType.DOUBLE)) return;

        double amount = pdc.get(plugin.getAmountKey(), PersistentDataType.DOUBLE);
        if (amount <= 0) return;


        ev.setCancelled(true);

        VaultHook.getEconomy().depositPlayer(player, amount);

        String formattedBalance = plugin.getFormatUtils().formatBalance(amount);
        languageManager.send(player, LanguageKeys.VOUCHER_CHECKED, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX), "%amount%", formattedBalance);


        if (playerItem.getAmount() > 1) {
            playerItem.setAmount(playerItem.getAmount() - 1);
        } else {
            if (ev.getHand() == EquipmentSlot.HAND) {
                player.getInventory().setItemInMainHand(null);
            } else if (ev.getHand() == EquipmentSlot.OFF_HAND) {
                player.getInventory().setItemInOffHand(null);
            }
        }

        if(SettingsConfig.getInstance().isTransactionLoggingEnabled()) {
            plugin.getTransactionLogger().appendLog(
                new Transaction(
                    player.getUniqueId().toString(),
                    "VOUCHER",
                    amount,
                    VaultHook.getEconomy().getBalance(player),
                    VaultHook.getEconomy().getBalance(player) + amount,
                    TransactionTypes.DEPOSIT,
                    System.currentTimeMillis()
                )
            );
        }
    }
}