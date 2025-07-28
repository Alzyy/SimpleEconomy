package it.alzy.simpleeconomy.simpleEconomy.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;

import it.alzy.simpleeconomy.simpleEconomy.SimpleEconomy;
import it.alzy.simpleeconomy.simpleEconomy.configurations.LangConfig;
import it.alzy.simpleeconomy.simpleEconomy.utils.ChatUtils;
import it.alzy.simpleeconomy.simpleEconomy.utils.VaultHook;

import net.milkbowl.vault.economy.EconomyResponse;

import java.util.concurrent.ExecutorService;

@CommandAlias("eco")
@Description("Manage players' economy balances. Includes subcommands to give, set, and remove money.")
public class ECOCommand extends BaseCommand {

    private final SimpleEconomy plugin;
    private final LangConfig config;
    private final ExecutorService executor;

    public ECOCommand() {
        this.plugin = SimpleEconomy.getInstance();
        this.config = LangConfig.getInstance();
        this.executor = plugin.getExecutor();
    }

    @Default
    public void root(Player player) {
        ChatUtils.send(player, "&#6B7280Usage &#374151| &#F9FAFB/eco &#6B7280<give|set|remove> <player> <amount>");
        return;
    }

    private OfflinePlayer getOfflinePlayerOrSendNotFound(Player sender, String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            ChatUtils.send(sender, config.PLAYER_NOT_FOUND, "%prefix%", config.PREFIX);
            return null;
        }
        return target;
    }

    private boolean validateAmount(Player player, double amount) {
        if (amount <= 0) {
            ChatUtils.send(player, config.INVALID_AMOUNT, "%prefix%", config.PREFIX);
            return false;
        }
        return true;
    }

    private void sendVaultError(Player player, String message) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            ChatUtils.send(player, "&cErrore Vault: " + message);
        });
    }

    @Subcommand("set")
    @CommandCompletion("@players")
    public void setSubCommand(Player player, String targetName, double amount) {
        if (!player.hasPermission("simpleconomy.eco.set")) {
            ChatUtils.send(player, config.NO_PERMISSION, "%prefix%", config.PREFIX);
            return;
        }

        if (amount < 0) {
            ChatUtils.send(player, config.INVALID_AMOUNT, "%prefix%", config.PREFIX);
            return;
        }

        OfflinePlayer target = getOfflinePlayerOrSendNotFound(player, targetName);
        if (target == null)
            return;

        executor.execute(() -> {
            var economy = VaultHook.getEconomy();
            if (economy == null) {
                sendVaultError(player, "Economy system not found");
                return;
            }

            double currentBalance = economy.getBalance(target);
            if (currentBalance == amount) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    ChatUtils.send(player, config.SET_SUCCESS,
                            "%prefix%", config.PREFIX,
                            "%amount%", plugin.getFormatUtils().formatBalance(amount),
                            "%target%", target.getName());
                });
                return;
            }

            EconomyResponse response;
            if (currentBalance > amount) {
                response = economy.withdrawPlayer(target, currentBalance - amount);
            } else {
                response = economy.depositPlayer(target, amount - currentBalance);
            }

            if (!response.transactionSuccess()) {
                sendVaultError(player, response.errorMessage);
                return;
            }

            plugin.getCacheMap().put(target.getUniqueId(), amount);
            plugin.getStorage().save(target.getUniqueId(), amount);

            String formattedAmount = plugin.getFormatUtils().formatBalance(amount);

            Bukkit.getScheduler().runTask(plugin, () -> {
                ChatUtils.send(player, config.SET_SUCCESS,
                        "%prefix%", config.PREFIX,
                        "%amount%", formattedAmount,
                        "%target%", target.getName());
            });
        });
    }

    @Subcommand("give")
    @CommandCompletion("@players")
    public void giveSubCommand(Player player, String targetName, double amount) {
        if (!player.hasPermission("simpleconomy.eco.give")) {
            ChatUtils.send(player, config.NO_PERMISSION, "%prefix%", config.PREFIX);
            return;
        }
        if (!validateAmount(player, amount))
            return;

        OfflinePlayer target = getOfflinePlayerOrSendNotFound(player, targetName);
        if (target == null)
            return;

        executor.execute(() -> {
            var economy = VaultHook.getEconomy();
            if (economy == null) {
                sendVaultError(player, "Economy system not found");
                return;
            }

            EconomyResponse response = economy.depositPlayer(target, amount);
            if (!response.transactionSuccess()) {
                sendVaultError(player, response.errorMessage);
                return;
            }

            double newBalance = economy.getBalance(target);
            plugin.getCacheMap().put(target.getUniqueId(), newBalance);
            plugin.getStorage().save(target.getUniqueId(), newBalance);

            String formattedAmount = plugin.getFormatUtils().formatBalance(amount);

            Bukkit.getScheduler().runTask(plugin, () -> {
                ChatUtils.send(player, config.GAVE_MONEY,
                        "%prefix%", config.PREFIX,
                        "%amount%", formattedAmount,
                        "%target%", target.getName());

                if (target.isOnline()) {
                    ChatUtils.send(target.getPlayer(), config.RECEIVED_MONEY,
                            "%prefix%", config.PREFIX,
                            "%amount%", formattedAmount,
                            "%source%", player.getName());
                }
            });
        });
    }

    @Subcommand("remove")
    @CommandCompletion("@players")
    public void removeSubCommand(Player player, String targetName, double amount) {
        if (!player.hasPermission("simpleconomy.eco.remove")) {
            ChatUtils.send(player, config.NO_PERMISSION, "%prefix%", config.PREFIX);
            return;
        }
        if (!validateAmount(player, amount))
            return;

        OfflinePlayer target = getOfflinePlayerOrSendNotFound(player, targetName);
        if (target == null)
            return;

        executor.execute(() -> {
            var economy = VaultHook.getEconomy();
            if (economy == null) {
                sendVaultError(player, "Economy system not found");
                return;
            }

            double currentBalance = economy.getBalance(target);
            if (currentBalance < amount) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    ChatUtils.send(player, config.NOT_ENOUGH_MONEY, "%prefix%", config.PREFIX);
                });
                return;
            }

            EconomyResponse withdraw = economy.withdrawPlayer(target, amount);
            if (!withdraw.transactionSuccess()) {
                sendVaultError(player, withdraw.errorMessage);
                return;
            }

            double newBalance = economy.getBalance(target);
            plugin.getCacheMap().put(target.getUniqueId(), newBalance);
            plugin.getStorage().save(target.getUniqueId(), newBalance);

            String formattedAmount = plugin.getFormatUtils().formatBalance(amount);

            Bukkit.getScheduler().runTask(plugin, () -> {
                ChatUtils.send(player, config.REMOVED_MONEY,
                        "%prefix%", config.PREFIX,
                        "%amount%", formattedAmount,
                        "%target%", target.getName());

                if (target.isOnline()) {
                    ChatUtils.send(target.getPlayer(), config.MONEY_REMOVED,
                            "%prefix%", config.PREFIX,
                            "%amount%", formattedAmount,
                            "%source%", player.getName());
                }
            });
        });
    }
}
