package it.alzy.simpleeconomy.plugin.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.configurations.SettingsConfig;
import it.alzy.simpleeconomy.plugin.i18n.LanguageManager;
import it.alzy.simpleeconomy.plugin.i18n.enums.LanguageKeys;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@CommandAlias("baltop|balancetop")
@Description("Displays the top balances in the server.")
public class BalTopCommand extends BaseCommand {

    private final SimpleEconomy plugin = SimpleEconomy.getInstance();
    private final LanguageManager languageManager = plugin.getLanguageManager();

    @Default
    @Syntax("[page]")
    public void onBaltop(Player player, @Default("1") int page) {
        plugin.getExecutor().execute(() -> {

            int pageSize = SettingsConfig.getInstance().getTopPlayersShown();
            if (pageSize <= 0) pageSize = 10;

            int fetchLimit = pageSize * 10;

            Map<String, Double> topMap = plugin.getStorage().getTopBalances("money", fetchLimit);

            int totalPages = (topMap.size() + pageSize - 1) / pageSize;
            if (totalPages == 0) {
                totalPages = 1;
            }

            if (page < 1 || page > totalPages) {
                languageManager.send(player, LanguageKeys.BALTOP_INVALID_PAGE, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX), "%total_pages%", String.valueOf(totalPages));
                return;
            }

            languageManager.send(player, LanguageKeys.BALTOP_HEADER, "%limit%", String.valueOf(pageSize));

            int start = (page - 1) * pageSize;
            AtomicInteger rank = new AtomicInteger(start + 1);

            topMap.entrySet().stream()
                    .skip(start)
                    .limit(pageSize)
                    .forEach(entry -> {
                        String playerUuidStr = entry.getKey();
                        double balance = entry.getValue();

                        String displayName;
                        try {
                            UUID uuid = UUID.fromString(playerUuidStr);
                            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                            displayName = offlinePlayer.getName() != null ? offlinePlayer.getName() : playerUuidStr;
                        } catch (IllegalArgumentException e) {
                            displayName = playerUuidStr;
                        }

                        String formattedBalance = plugin.getFormatUtils().formatBalance(balance);
                        languageManager.send(player, LanguageKeys.BALTOP_ENTRY, "%position%", String.valueOf(rank.getAndIncrement()), "%player%", displayName, "%balance%", formattedBalance);
                    });

            languageManager.send(player, LanguageKeys.BALTOP_FOOTER, "%currentPage%", String.valueOf(page), "%maxPage%", String.valueOf(totalPages));
        });
    }

    @Subcommand("refresh")
    @CommandPermission("simpleconomy.baltop.refresh")
    @Description("Refreshes the top balances list by forcing a save of cached data.")
    public void onRefresh(Player player) {
        plugin.getExecutor().execute(() -> {
            plugin.getStorage().bulkSave();
            languageManager.send(player, LanguageKeys.BALTOP_REFRESHED, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
        });
    }
}