package it.alzy.simpleeconomy.plugin.commands;

import java.math.BigDecimal;

import co.aikar.commands.annotation.Optional;
import org.bukkit.entity.Player;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.configurations.LangConfig;
import it.alzy.simpleeconomy.plugin.configurations.SettingsConfig;
import it.alzy.simpleeconomy.plugin.utils.ChatUtils;
import it.alzy.simpleeconomy.plugin.utils.VaultHook;


@CommandAlias("voucher|withdraw")
@Description("Creates a voucher with an amount")
public class VoucherCommand extends BaseCommand {
    

    private final LangConfig config = LangConfig.getInstance();

    @Default
    public void root(Player player, @Optional Double amount) {
        if(amount == null) {
            ChatUtils.send(player, config.USAGE_VOUCHER, "%prefix%", config.PREFIX);
            return;
        }
        if(!validateAmount(amount)) {
            ChatUtils.send(player, config.NEGATIVE_AMOUNT, "%prefix%", config.PREFIX);
            return;
        }
        if(!VaultHook.getEconomy().has(player, amount)) {
            ChatUtils.send(player, config.NOT_ENOUGH_MONEY, "%prefix%", config.PREFIX);
            return;
        }
        VaultHook.getEconomy().withdrawPlayer(player, amount);
        SimpleEconomy.getInstance().getItemUtils().createVoucherAndGive(player, amount);
    } 

    public boolean validateAmount(double amount) {
        if (!Double.isFinite(amount))
            return false;
        if (amount <= 0)
            return false;

        double max = SettingsConfig.getInstance().getMaxVoucherAmount();
        if (max != 0 && amount > max)
            return false;

        BigDecimal bd = BigDecimal.valueOf(amount);
        return bd.scale() <= 2;
    }

}
