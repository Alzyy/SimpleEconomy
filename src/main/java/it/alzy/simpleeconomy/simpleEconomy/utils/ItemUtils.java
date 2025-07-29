package it.alzy.simpleeconomy.simpleEconomy.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import it.alzy.simpleeconomy.simpleEconomy.SimpleEconomy;
import it.alzy.simpleeconomy.simpleEconomy.configurations.LangConfig;
import it.alzy.simpleeconomy.simpleEconomy.configurations.SettingsConfig;
import net.kyori.adventure.text.Component;

public class ItemUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd:MM:yyyy HH:mm");
    private static final LangConfig config = LangConfig.getInstance();
    private static final SettingsConfig SETTINGS = SettingsConfig.getInstance();
    private static final SimpleEconomy plugin = SimpleEconomy.getInstance();

    public ItemUtils() {
    }

    public void createVoucherAndGive(Player player, double amount) {
        if (amount <= 0) {
            ChatUtils.send(player, config.INVALID_AMOUNT, "%prefix%", config.PREFIX);
            return;
        }

        if (isInventoryFull(player)) {
            ChatUtils.send(player, config.INVENTORY_FULL, "%prefix%", config.PREFIX);
            return;
        }

        Material material = Material.getMaterial(SETTINGS.getVoucherMaterial());
        if (material == null)
            material = Material.PAPER;

        ItemStack voucher = new ItemStack(material, 1);
        ItemMeta meta = voucher.getItemMeta();
        if (meta == null) {
            ChatUtils.send(player, config.INVALID_AMOUNT);
            return;
        }

        meta.displayName(ChatUtils.createComponent(
                SETTINGS.getVoucherItemName(),
                "%playerName%", player.getName()));

        List<String> preLore = SETTINGS.getVoucherLore();
        List<Component> loreComponents = preLore.stream()
                .map(line -> ChatUtils.createComponent(
                        line,
                        "%amount%", plugin.getFormatUtils().formatBalance(amount),
                        "%creationDate%", today()))
                .toList();

        meta.lore(loreComponents);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(plugin.getAmountKey(), PersistentDataType.DOUBLE, amount);
        pdc.set(plugin.getUuidKey(), PersistentDataType.STRING, UUID.randomUUID().toString());

        voucher.setItemMeta(meta);

        player.getInventory().addItem(voucher);
        ChatUtils.send(player, config.VOUCHER_CREATED, "%prefix%" ,config.PREFIX, "%amount%", plugin.getFormatUtils().formatBalance(amount));
    }

    public boolean isInventoryFull(Player player) {
        return player.getInventory().firstEmpty() == -1;
    }

    public String today() {
        return LocalDateTime.now().format(DATE_FORMATTER);
    }
}
