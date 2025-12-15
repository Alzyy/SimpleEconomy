package it.alzy.simpleeconomy.plugin.utils;

import it.alzy.simpleeconomy.api.SimpleEconomyAPI;
import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.configurations.SettingsConfig;
import it.alzy.simpleeconomy.plugin.i18n.LanguageManager;
import it.alzy.simpleeconomy.plugin.i18n.enums.LanguageKeys;
import it.alzy.simpleeconomy.plugin.records.Transaction;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class ItemUtils {

    private final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final SimpleEconomy plugin;
    private final SettingsConfig settings;
    private final LanguageManager languageManager;

    public ItemUtils() {
        this.plugin = SimpleEconomy.getInstance();
        this.settings = SettingsConfig.getInstance();
        this.languageManager = plugin.getLanguageManager();
    }

    public void createVoucherAndGive(Player player, double amount) {
        if (amount <= 0) {
            languageManager.send(player, LanguageKeys.INVALID_AMOUNT, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
            return;
        }

        if (player.getInventory().firstEmpty() == -1) {
            languageManager.send(player, LanguageKeys.INVENTORY_FULL, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
            return;
        }

        Material material = Optional.ofNullable(Material.getMaterial(settings.getVoucherMaterial().toUpperCase())).orElse(Material.PAPER);

        ItemStack voucher = new ItemStack(material);
        ItemMeta meta = voucher.getItemMeta();

        if (meta == null) {
            return;
        }

        String formattedAmount = plugin.getFormatUtils().formatBalance(amount);
        String date = LocalDateTime.now().format(DATE_FORMATTER);
        List<String> rawLore = settings.getVoucherLore();

        if (plugin.isPaper()) {
            meta.displayName(ChatUtils.createComponent(settings.getVoucherItemName(), "%playerName%", player.getName()));

            List<Component> lore = rawLore.stream()
                    .map(line -> ChatUtils.createComponent(line, "%amount%", formattedAmount, "%creationDate%", date)).collect(Collectors.toList());
            meta.lore(lore);
        } else {
            meta.setDisplayName(formatString(settings.getVoucherItemName(), player.getName(), formattedAmount, date));

            List<String> lore = rawLore.stream()
                    .map(line -> formatString(line, player.getName(), formattedAmount, date))
                    .collect(Collectors.toList());

            meta.setLore(lore);
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(plugin.getAmountKey(), PersistentDataType.DOUBLE, amount);
        pdc.set(plugin.getUuidKey(), PersistentDataType.STRING, UUID.randomUUID().toString());

        voucher.setItemMeta(meta);
        player.getInventory().addItem(voucher);

        languageManager.send(player, LanguageKeys.VOUCHER_CREATED, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX), "%amount%", formattedAmount);
    }

    private String formatString(String text, String playerName, String amount, String date) {
        return ChatColor.translateAlternateColorCodes('&', text
                .replace("%playerName%", playerName)
                .replace("%amount%", amount)
                .replace("%creationDate%", date));
    }
}