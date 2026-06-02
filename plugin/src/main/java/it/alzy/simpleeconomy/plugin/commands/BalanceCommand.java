package it.alzy.simpleeconomy.plugin.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.configurations.SettingsConfig;
import it.alzy.simpleeconomy.plugin.i18n.LanguageManager;
import it.alzy.simpleeconomy.plugin.i18n.enums.LanguageKeys;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

@CommandAlias("balance|bal|money")
@Description("Displays the balance of a player. Shows your own balance if no player is specified. Viewing others' balances requires permission.")
public class BalanceCommand extends BaseCommand {

    private final SimpleEconomy plugin = SimpleEconomy.getInstance();
    private final LanguageManager languageManager = plugin.getLanguageManager();

    @Default
    public void onBalance(Player player, @Optional String targetName) {

        if (targetName == null || targetName.isEmpty()) {
            double balance = getCachedBalance(player.getUniqueId());
            String formattedBalance = plugin.getFormatUtils().formatBalance(balance);
            languageManager.send(player, LanguageKeys.BALANCE_CHECK_SELF, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX), "%balance%", formattedBalance);
            return;
        }

        if (!player.hasPermission("simpleconomy.balance.others")) {
            languageManager.send(player, LanguageKeys.NO_PERMISSION, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            languageManager.send(player, LanguageKeys.PLAYER_NOT_FOUND, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX), "%target%", targetName);
            return;
        }

        String resolvedName = target.getName() != null ? target.getName() : targetName;

        if (plugin.getCache().contains(target.getUniqueId())) {
            double balance = getCachedBalance(target.getUniqueId());
            String formattedBalance = plugin.getFormatUtils().formatBalance(balance);
            languageManager.send(player, LanguageKeys.BALANCE_CHECK_OTHER, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX), "%balance%", formattedBalance, "%target%", resolvedName);
        } else {
            plugin.getStorage().getBalance(target.getUniqueId(), "money").thenAccept(dbBalance -> {
                double finalBalance = dbBalance != null ? dbBalance : SettingsConfig.getInstance().startingBalance();
                String formattedBalance = plugin.getFormatUtils().formatBalance(finalBalance);
                languageManager.send(player, LanguageKeys.BALANCE_CHECK_OTHER, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX), "%balance%", formattedBalance, "%target%", resolvedName);
            });
        }
    }


    private double getCachedBalance(UUID uuid) {
        Map<String, Double> balances = plugin.getCache().get(uuid);
        if (balances != null && balances.containsKey("money")) {
            return balances.get("money");
        }
        return SettingsConfig.getInstance().startingBalance();
    }
}