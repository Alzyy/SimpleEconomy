package it.alzy.simpleeconomy.plugin.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import it.alzy.simpleeconomy.api.EconomyProvider;
import it.alzy.simpleeconomy.api.SimpleEconomyAPI;
import it.alzy.simpleeconomy.api.TransactionTypes;
import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.configurations.SettingsConfig;
import it.alzy.simpleeconomy.plugin.i18n.LanguageManager;
import it.alzy.simpleeconomy.plugin.i18n.enums.LanguageKeys;
import it.alzy.simpleeconomy.plugin.utils.TransactionHelper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

@CommandAlias("voucher|withdraw")
@Description("Creates a voucher with an amount")
public class VoucherCommand extends BaseCommand {
    
    private final SimpleEconomy plugin = SimpleEconomy.getInstance();
    private final LanguageManager languageManager = plugin.getLanguageManager();

    @Default
    public void root(Player player, @Optional Double amount) {
        if (amount == null) {
            languageManager.send(player, LanguageKeys.VOUCHER_USAGE, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
            return;
        }

        final EconomyProvider provider = SimpleEconomyAPI.getProvider();

        TransactionHelper helper = plugin.getTransactionHelper();

        if (!helper.validateAmount(player, amount)) {
            return;
        }

        double maxVoucher = SettingsConfig.getInstance().getMaxVoucherAmount();
        if (maxVoucher > 0 && amount > maxVoucher) {
            languageManager.send(player, LanguageKeys.INVALID_AMOUNT, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
            return;
        }

        if (player.getInventory().firstEmpty() == -1) {
            languageManager.send(player, LanguageKeys.INVENTORY_FULL, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
            return;
        }

        String currency = "money";
        UUID uuid = player.getUniqueId();

        provider.getBalance(uuid, currency).thenAccept(balanceBefore -> {
            
            if (balanceBefore < amount) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    languageManager.send(player, LanguageKeys.NOT_ENOUGH_MONEY, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
                });
                return;
            }

            provider.detract(uuid, currency, amount).thenAccept(success -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!success) {
                        languageManager.send(player, LanguageKeys.NOT_ENOUGH_MONEY, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
                        return;
                    }

                    double balanceAfter = balanceBefore - amount;

                    plugin.getItemUtils().createVoucherAndGive(player, amount);

                    String formattedAmount = plugin.getFormatUtils().formatBalance(amount);
                    languageManager.send(player, LanguageKeys.REMOVED_MONEY, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX), "%amount%", formattedAmount, "%target%", player.getName());

                    helper.commitAsync(
                            uuid,
                            uuid,
                            amount,
                            balanceBefore,
                            balanceAfter,
                            TransactionTypes.WITHDRAW,
                            "withdraw",
                            player.getName(),
                            null
                    );
                });
            }).exceptionally(ex -> {
                plugin.getLogger().severe("Error processing voucher command for " + player.getName() + ": " + ex.getMessage());
                return null;
            });
        }).exceptionally(ex -> {
            plugin.getLogger().severe("Error fetching balance for " + player.getName() + " during voucher creation: " + ex.getMessage());
            return null;
        });
    }
}