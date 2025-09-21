package it.alzy.simpleeconomy.simpleEconomy.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import it.alzy.simpleeconomy.simpleEconomy.SimpleEconomy;
import it.alzy.simpleeconomy.simpleEconomy.configurations.LangConfig;
import it.alzy.simpleeconomy.simpleEconomy.configurations.SettingsConfig;
import it.alzy.simpleeconomy.simpleEconomy.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@CommandAlias("baltop|balancetop")
@Description("Displays the top balances in the server.")
public class BalTopCommand extends BaseCommand {

    SimpleEconomy plugin = SimpleEconomy.getInstance();
    @Default
    public void onBaltop(Player player) {

        plugin.getExecutor().execute(() -> {
            int limit = SettingsConfig.getInstance().getTopPlayersShown();

            sendSync(player, LangConfig.getInstance().BALTOP_HEADER, "%limit%", limit);

            if (plugin.getTopMap() == null || plugin.getTopMap().isEmpty()) {
                System.out.println("Refreshing top balances for " + player.getName() + " on-demand.");
                plugin.getStorage().getTopBalances(limit);
            }

            AtomicInteger position = new AtomicInteger(1);

            plugin.getTopMap().forEach((uuidStr, balance) -> {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
                    String name = (offline != null && offline.getName() != null)
                            ? offline.getName()
                            : "Unknown";

                    sendSync(player,
                            LangConfig.getInstance().BALTOP_ENTRY,
                            "%position%", String.valueOf(position.getAndIncrement()),
                            "%player%", name,
                            "%balance%", plugin.getFormatUtils().formatBalance(balance));

                } catch (IllegalArgumentException e) {
                    sendSync(player,
                            LangConfig.getInstance().BALTOP_ENTRY,
                            "%position%", String.valueOf(position.getAndIncrement()),
                            "%player%", "Invalid-UUID",
                            "%balance%", plugin.getFormatUtils().formatBalance(balance));
                }
            });
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
            sendSync(player, LangConfig.getInstance().BALTOP_REFRESHED, "%prefix%", LangConfig.getInstance().PREFIX);
        });
    }
    private void sendSync(Player player, String message, Object... placeholders) {
        Bukkit.getScheduler().runTask(SimpleEconomy.getInstance(),
                () -> ChatUtils.send(player, message, placeholders));
    }
}
