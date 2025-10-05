package it.alzy.simpleeconomy.plugin.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.configurations.LangConfig;
import it.alzy.simpleeconomy.plugin.configurations.SettingsConfig;
import it.alzy.simpleeconomy.plugin.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@CommandAlias("baltop|balancetop")
@Description("Displays the top balances in the server.")
public class BalTopCommand extends BaseCommand {

    SimpleEconomy plugin = SimpleEconomy.getInstance();
    @Default
    @Syntax("[page]")
    public void onBaltop(Player player, @Default("1") int page) {
        plugin.getExecutor().execute(() -> {
            int limit = SettingsConfig.getInstance().getTopPlayersShown();


            Map<String, Double> topMap = plugin.getStorage().getAllBalances();
            int totalPages = (topMap.size() + limit - 1 ) / limit;
            if (page < 1 || page > totalPages) {
                ChatUtils.send(player, LangConfig.getInstance().INVALID_BALTOP_PAGE, "%prefix%", LangConfig.getInstance().PREFIX, "%total_pages%", totalPages);
                return;
            }

            ChatUtils.send(player, LangConfig.getInstance().BALTOP_HEADER, "%limit%", limit);

            int start = (page - 1) * limit;
            AtomicInteger rank = new AtomicInteger(start + 1);
            topMap.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .skip(start)
                .limit(limit)
                .forEach(entry -> {
                    String playerName = entry.getKey();
                    double balance = entry.getValue();
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(playerName));
                    String displayName = offlinePlayer != null && offlinePlayer.getName() != null ? offlinePlayer.getName() : playerName;
                    String formattedBalance = plugin.getFormatUtils().formatBalance(balance);
                    ChatUtils.send(player, LangConfig.getInstance().BALTOP_ENTRY,
                            "%position%", String.valueOf(rank.getAndIncrement()),
                            "%player%", displayName,
                            "%balance%", formattedBalance);
                });
            ChatUtils.send(player, LangConfig.getInstance().BALTOP_FOOTER,  "%currentPage%", String.valueOf(page), "%maxPage%", String.valueOf(totalPages));
        });
    }


    @Subcommand("refresh")
    @CommandPermission("simpleconomy.baltop.refresh")
    @Description("Refreshes the top balances list.")
    public void onRefresh(Player player) {
        plugin.getExecutor().execute(() -> {
            plugin.getStorage().bulkSave();
            ConcurrentMap<String, Double> topMap = new ConcurrentHashMap<>(plugin.getStorage().getTopBalances(
                    SettingsConfig.getInstance().getTopPlayersShown()
            ));
            plugin.setTopMap(topMap);
            ChatUtils.send(player, LangConfig.getInstance().BALTOP_REFRESHED, "%prefix%", LangConfig.getInstance().PREFIX);
        });
    }
}
