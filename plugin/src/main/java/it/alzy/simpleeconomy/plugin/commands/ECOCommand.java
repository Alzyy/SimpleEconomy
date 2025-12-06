package it.alzy.simpleeconomy.plugin.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.configurations.LangConfig;
import it.alzy.simpleeconomy.plugin.utils.ChatUtils;
import it.alzy.simpleeconomy.plugin.utils.VaultHook;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@CommandAlias("eco")
@Description("Manage players' economy balances.")
public class ECOCommand extends BaseCommand {

    private final SimpleEconomy plugin;
    private final LangConfig config;

    public ECOCommand() {
        this.plugin = SimpleEconomy.getInstance();
        this.config = LangConfig.getInstance();
    }

    private enum EcoAction { GIVE, SET, REMOVE }

    @Default
    public void root(CommandSender player) {
        ChatUtils.send(player, config.USAGE_ECO, "%prefix%", config.PREFIX);
    }

    @Subcommand("set")
    @CommandCompletion("@players|@a|@p|@r")
    public void set(CommandSender sender, String targetName, double amount) {
        execute(sender, targetName, amount, EcoAction.SET);
    }

    @Subcommand("give")
    @CommandCompletion("@players|@a|@p|@r")
    public void give(CommandSender sender, String targetName, double amount) {
        execute(sender, targetName, amount, EcoAction.GIVE);
    }

    @Subcommand("remove")
    @CommandCompletion("@players|@a|@p|@r")
    public void remove(CommandSender sender, String targetName, double amount) {
        execute(sender, targetName, amount, EcoAction.REMOVE);
    }

    private void execute(CommandSender sender, String targetName, double amount, EcoAction action) {
        if (!sender.hasPermission("simpleconomy.eco." + action.name().toLowerCase())) {
            ChatUtils.send(sender, config.NO_PERMISSION, "%prefix%", config.PREFIX);
            return;
        }

        if (!isValidAmount(sender, amount, action)) return;

        Economy economy = VaultHook.getEconomy();
        if (economy == null) {
            return;
        }

        Collection<OfflinePlayer> targets = resolveTargets(sender, targetName);
        if (targets.isEmpty()) return;

        final String formattedAmount = plugin.getFormatUtils().formatBalance(amount);

        for (OfflinePlayer target : targets) {
            EconomyResponse response;

            switch (action) {
                case GIVE -> response = economy.depositPlayer(target, amount);

                case REMOVE -> {
                    if (!economy.has(target, amount)) {
                        ChatUtils.send(sender, config.NOT_ENOUGH_MONEY, "%prefix%", config.PREFIX);
                        continue;
                    }
                    response = economy.withdrawPlayer(target, amount);
                }

                case SET -> {
                    double current = economy.getBalance(target);
                    if (current < amount) {
                        response = economy.depositPlayer(target, amount - current);
                    } else if (current > amount) {
                        response = economy.withdrawPlayer(target, current - amount);
                    } else {
                        response = new EconomyResponse(0, current, EconomyResponse.ResponseType.SUCCESS, null);
                    }
                }
                default -> response = new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Unknown Error");
            }

            if (response.transactionSuccess()) {
                handleSuccess(sender, target, response.balance, formattedAmount, action);
            } else {
                ChatUtils.send(sender, "&cError: " + response.errorMessage, "%prefix%", config.PREFIX);
            }
        }
    }

    private void handleSuccess(CommandSender sender, OfflinePlayer target, double newBalance, String formattedAmount, EcoAction action) {
        plugin.getCacheMap().put(target.getUniqueId(), newBalance);
        plugin.getExecutor().execute(() -> plugin.getStorage().save(target.getUniqueId(), newBalance));

        String senderMsg = switch (action) {
            case GIVE -> config.GAVE_MONEY;
            case REMOVE -> config.REMOVED_MONEY;
            case SET -> config.SET_SUCCESS;
        };

        ChatUtils.send(sender, senderMsg, "%prefix%", config.PREFIX, "%amount%", formattedAmount, "%target%", target.getName());

        if (action != EcoAction.SET && target.isOnline() && target.getPlayer() != null) {
            String targetMsg = (action == EcoAction.GIVE) ? config.RECEIVED_MONEY : config.MONEY_REMOVED;
            ChatUtils.send(target.getPlayer(), targetMsg, "%prefix%", config.PREFIX, "%amount%", formattedAmount, "%source%", sender.getName());
        }
    }

    private boolean isValidAmount(CommandSender sender, double amount, EcoAction action) {
        if (!Double.isFinite(amount)) {
            ChatUtils.send(sender, config.INVALID_AMOUNT, "%prefix%", config.PREFIX);
            return false;
        }

        boolean isInvalid = (action == EcoAction.SET) ? (amount < 0) : (amount <= 0);

        if (isInvalid) {
            ChatUtils.send(sender, config.INVALID_AMOUNT, "%prefix%", config.PREFIX);
            return false;
        }

        if (BigDecimal.valueOf(amount).scale() > 2) {
            ChatUtils.send(sender, config.INVALID_AMOUNT, "%prefix%", config.PREFIX);
            return false;
        }
        return true;
    }

    private Collection<OfflinePlayer> resolveTargets(CommandSender sender, String targetName) {
        if (targetName.length() == 2 && targetName.charAt(0) == '@') {
            char selector = Character.toLowerCase(targetName.charAt(1));
            return switch (selector) {
                case 'a' -> new ArrayList<>(Bukkit.getOnlinePlayers());
                case 'p' -> (sender instanceof Player p) ? Collections.singletonList(p) : Collections.emptyList();
                case 'r' -> {
                    Collection<? extends Player> online = Bukkit.getOnlinePlayers();
                    if (online.isEmpty()) yield Collections.emptyList();
                    yield Collections.singletonList(online.stream()
                            .skip(ThreadLocalRandom.current().nextInt(online.size()))
                            .findFirst().orElse(null));
                }
                default -> getOfflinePlayerFallback(sender, targetName);
            };
        }
        return getOfflinePlayerFallback(sender, targetName);
    }

    private Collection<OfflinePlayer> getOfflinePlayerFallback(CommandSender sender, String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            ChatUtils.send(sender, config.PLAYER_NOT_FOUND, "%prefix%", config.PREFIX);
            return Collections.emptyList();
        }
        return Collections.singletonList(target);
    }
}