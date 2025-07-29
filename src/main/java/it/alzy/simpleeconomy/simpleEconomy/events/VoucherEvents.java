package it.alzy.simpleeconomy.simpleEconomy.events;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import it.alzy.simpleeconomy.simpleEconomy.SimpleEconomy;
import it.alzy.simpleeconomy.simpleEconomy.configurations.LangConfig;
import it.alzy.simpleeconomy.simpleEconomy.configurations.SettingsConfig;
import it.alzy.simpleeconomy.simpleEconomy.utils.ChatUtils;
import it.alzy.simpleeconomy.simpleEconomy.utils.VaultHook;

public class VoucherEvents implements Listener {


    private final SimpleEconomy plugin = SimpleEconomy.getInstance();

    @EventHandler
    public void playerInteract(PlayerInteractEvent ev) {
        Player player = ev.getPlayer();
        ItemStack playerItem = ev.getItem();
        if (playerItem == null)
            return;

        Material voucherMaterial = Material.getMaterial(SettingsConfig.getInstance().getVoucherMaterial());
        if (voucherMaterial == null)
            return; 

        if (playerItem.getType() != voucherMaterial)
            return;
        if (!playerItem.hasItemMeta())
            return;

        ItemMeta meta = playerItem.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (!pdc.has(plugin.getUuidKey(), PersistentDataType.STRING))
            return;
        if (!pdc.has(plugin.getAmountKey(), PersistentDataType.DOUBLE))
            return;

        double amount = pdc.get(plugin.getAmountKey(), PersistentDataType.DOUBLE);
        if (amount <= 0)
            return;

        VaultHook.getEconomy().depositPlayer(player, amount);

        String formattedBalance = plugin.getFormatUtils().formatBalance(amount);
        ChatUtils.send(player, LangConfig.getInstance().VOUCHER_CHECKED, "%prefix%", LangConfig.getInstance().PREFIX, "%amount%", formattedBalance);

        int newAmount = playerItem.getAmount() - 1;
        if (newAmount > 0) {
            playerItem.setAmount(newAmount);
        } else {
            player.getInventory().removeItem(playerItem);
        }
    }

}
