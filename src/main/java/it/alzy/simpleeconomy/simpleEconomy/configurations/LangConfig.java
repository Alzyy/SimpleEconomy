package it.alzy.simpleeconomy.simpleEconomy.configurations;

import net.pino.simpleconfig.BaseConfig;
import net.pino.simpleconfig.annotations.Config;
import net.pino.simpleconfig.annotations.ConfigFile;
import net.pino.simpleconfig.annotations.inside.Path;

@Config
@ConfigFile("lang.yml")
public class LangConfig extends BaseConfig {

    private static LangConfig instance = null;

    @Path("prefix")
    public String PREFIX = "&#22C55Eѕᴇ ● &#9CA3AF";

    @Path("messages.gave_money")
    public String GAVE_MONEY = "%prefix% &#A7F3D0✔ &#6B7280You sent &#22C55E%amount% &#6B7280to &#F9FAFB%target%";

    @Path("messages.received_money")
    public String RECEIVED_MONEY = "%prefix% &#A7F3D0✔ &#6B7280You received &#22C55E%amount% &#6B7280from &#F9FAFB%source%";

    @Path("messages.invalid_amount")
    public String INVALID_AMOUNT = "%prefix% &#F87171✘ &#EF4444Invalid amount &#6B7280| &#FBBF24Enter a valid number";

    @Path("messages.negative_amount")
    public String NEGATIVE_AMOUNT = "%prefix% &#F87171✘ &#EF4444Amount must be positive &#6B7280| &#FBBF24Use a number above zero";

    @Path("messages.reload_success")
    public String RELOAD_SUCCESS = "%prefix% &#86EFAC✔ &#22C55ESuccessfully reloaded &#6B7280| Settings applied";

    @Path("messages.no_permission")
    public String NO_PERMISSION = "%prefix% &#F87171✘ &#EF4444You don't have permission &#6B7280to use this command";

    @Path("messages.player_not_found")
    public String PLAYER_NOT_FOUND = "%prefix% &#F87171✘ &#EF4444Player not found &#6B7280| &#FBBF24Check the name and try again";

    @Path("messages.balance_check_self")
    public String BALANCE_CHECK_SELF = "%prefix% &#6B7280Balance &#374151| &#22C55E%balance%";

    @Path("messages.balance_check_other")
    public String BALANCE_CHECK_OTHER = "%prefix% &#6B7280%target%'s Balance &#374151| &#22C55E%balance%";

    @Path("messages.eco.set_success")
    public String SET_SUCCESS = "%prefix% &#86EFAC✔ &#6B7280Set &#F9FAFB%target%'s &#6B7280balance to &#22C55E%amount%";

    @Path("messages.eco.usage")
    public String USAGE_ECO = "%prefix% &#6B7280Usage &#374151| &#F9FAFB/eco &#6B7280<give|set|remove> <player> <amount>";

    @Path("messages.not_enough_money")
    public String NOT_ENOUGH_MONEY = "%prefix% &#EF4444✘ &#6B7280Insufficient funds &#374151| &#FBBF24Balance too low";

    @Path("messages.removed_money")
    public String REMOVED_MONEY = "%prefix% &#FCA5A5✔ &#6B7280You removed &#F9FAFB%amount% &#6B7280from &#F9FAFB%target%";

    @Path("messages.money_removed")
    public String MONEY_REMOVED = "%prefix% &#FCA5A5✔ &#F9FAFB%amount% &#6B7280was removed from your account by &#F9FAFB%source%";

    @Path("messages.self-command")
    public String SELF = "%prefix% &#F97316✘ &#EF4444You cannot use this command on yourself";

    @Path("messages.inventory-full")
    public String INVENTORY_FULL = "%prefix% &#F97316✘ &#EF4444Your inventory is full!";

    @Path("messages.voucher-created")
    public String VOUCHER_CREATED = "%prefix% &#22C55E✔ &#6B7280Voucher created successfully for &#F9FAFB%amount%";

    @Path("messages.voucher-checked")
    public String VOUCHER_CHECKED = "%prefix% &#22C55Eℹ &#6B7280You have checked a voucher worth &#F9FAFB%amount%";

    @Path("messages.baltop.header")
    public String BALTOP_HEADER = "&#22C55E💰 &#6B7280Top &#F9FAFB%limit% &#6B7280richest players";

    @Path("messages.baltop.entry")
    public String BALTOP_ENTRY = "&#6B7280#%position% &#F9FAFB%player% &#6B7280- &#22C55E%balance%";

    @Path("messages.baltop.refreshed")
    public String BALTOP_REFRESHED = "%prefix% &#22C55E✔ &#6B7280Top balances list refreshed";



    public static LangConfig getInstance() {
        if (instance == null)
            instance = new LangConfig();
        return instance;
    }
}
