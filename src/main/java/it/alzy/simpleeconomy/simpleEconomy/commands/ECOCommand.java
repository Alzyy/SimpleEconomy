package it.alzy.simpleeconomy.simpleEconomy.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

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

import java.math.BigDecimal;
import java.util.*;
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
    public void root(CommandSender player) {
        ChatUtils.send(player, config.USAGE_ECO, "%prefix%", config.PREFIX);
        return;
    }

    private OfflinePlayer getOfflinePlayerOrSendNotFound(CommandSender sender, String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            ChatUtils.send(sender, config.PLAYER_NOT_FOUND, "%prefix%", config.PREFIX);
            return null;
        }
        return target;
    }


    private Collection<? extends OfflinePlayer> resolveTargets(CommandSender sender, String targetName) {
        if (targetName.equalsIgnoreCase("@a")) { // all players
            return Bukkit.getOnlinePlayers();
        }

        if (targetName.equalsIgnoreCase("@p")) { //nearest or self
            if (sender instanceof OfflinePlayer p) {
                return List.of(p);
            }
            return List.of();
        }

        if (targetName.equalsIgnoreCase("@r")) { //random player
            var online = Bukkit.getOnlinePlayers();
            if (online.isEmpty()) return List.of();
            int idx = new Random().nextInt(online.size());
            return List.of(online.stream().toList().get(idx));
        }

        OfflinePlayer target = getOfflinePlayerOrSendNotFound(sender, targetName); //fallback
        if (target == null) return List.of();
        return List.of(target);
    }

    private boolean validateAmount(CommandSender player, double amount) {
        if (!Double.isFinite(amount)) {
            ChatUtils.send(player, config.INVALID_AMOUNT, "%prefix%", config.PREFIX);
            return false;
        }

        if (amount <= 0) {
            ChatUtils.send(player, config.INVALID_AMOUNT, "%prefix%", config.PREFIX);
            return false;
        }

        BigDecimal bd = BigDecimal.valueOf(amount);
        if (bd.scale() > 2) {
            ChatUtils.send(player, config.INVALID_AMOUNT, "%prefix%", config.PREFIX);
            return false;
        }

        return true;
    }

    @Subcommand("set")
    @CommandCompletion("@players|@a|@p|@r")
    public void setSubCommand(CommandSender player, String targetName, double amount) {
        if (!player.hasPermission("simpleconomy.eco.set")) {
            ChatUtils.send(player, config.NO_PERMISSION, "%prefix%", config.PREFIX);
            return;
        }

        if (amount < 0) {
            ChatUtils.send(player, config.INVALID_AMOUNT, "%prefix%", config.PREFIX);
            return;
        }

        var targets = resolveTargets(player, targetName);
        if (targets.isEmpty()) {
            ChatUtils.send(player, config.PLAYER_NOT_FOUND, "%prefix%", config.PREFIX);
            return;
        }

        for (OfflinePlayer target : targets) {
            executor.execute(() -> {
                var economy = VaultHook.getEconomy();
                if (economy == null) {
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
    }

    @Subcommand("give")
    @CommandCompletion("@players|@a|@p|@r")
    public void giveSubCommand(CommandSender player, String targetName, double amount) {
        if (!player.hasPermission("simpleconomy.eco.give")) {
            ChatUtils.send(player, config.NO_PERMISSION, "%prefix%", config.PREFIX);
            return;
        }
        if (!validateAmount(player, amount))
            return;

        var targets = resolveTargets(player, targetName);
        if (targets.isEmpty()) {
            ChatUtils.send(player, config.PLAYER_NOT_FOUND, "%prefix%", config.PREFIX);
            return;
        }

        for (OfflinePlayer target : targets) {
            executor.execute(() -> {
                var economy = VaultHook.getEconomy();
                if (economy == null) {
                    return;
                }

                EconomyResponse response = economy.depositPlayer(target, amount);
                if (!response.transactionSuccess()) {
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
    }

    @Subcommand("remove")
    @CommandCompletion("@players|@a|@p|@r")
    public void removeSubCommand(CommandSender player, String targetName, double amount) {
        if (!player.hasPermission("simpleconomy.eco.remove")) {
            ChatUtils.send(player, config.NO_PERMISSION, "%prefix%", config.PREFIX);
            return;
        }
        if (!validateAmount(player, amount))
            return;

        var targets = resolveTargets(player, targetName);
        if (targets.isEmpty()) {
            ChatUtils.send(player, config.PLAYER_NOT_FOUND, "%prefix%", config.PREFIX);
            return;
        }

        for (OfflinePlayer target : targets) {
            executor.execute(() -> {
                var economy = VaultHook.getEconomy();
                if (economy == null) {
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
}
