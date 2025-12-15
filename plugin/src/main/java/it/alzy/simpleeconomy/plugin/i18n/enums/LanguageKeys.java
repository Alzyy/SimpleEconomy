package it.alzy.simpleeconomy.plugin.i18n.enums;

public enum LanguageKeys {

    PREFIX("prefix"),

    RELOAD_SUCCESS("messages.system.reload_success"),
    NO_PERMISSION("messages.system.no_permission"),
    PLAYER_NOT_FOUND("messages.system.player_not_found"),
    INVENTORY_FULL("messages.system.inventory_full"),
    SELF_COMMAND("messages.system.self_command_error"),

    INVALID_AMOUNT("messages.errors.invalid_amount"),
    NEGATIVE_AMOUNT("messages.errors.negative_amount"),
    AMOUNT_EXCEEDS_LIMIT("messages.errors.amount_exceeds_limit"),
    NOT_ENOUGH_MONEY("messages.errors.not_enough_money"),

    BALANCE_CHECK_SELF("messages.balance.self"),
    BALANCE_CHECK_OTHER("messages.balance.other"),

    PAY_USAGE("messages.pay.usage"),
    GAVE_MONEY("messages.pay.sent"),
    RECEIVED_MONEY("messages.pay.received"),

    ECO_USAGE("messages.eco.usage"),
    ECO_SET_SUCCESS("messages.eco.set_success"),
    REMOVED_MONEY("messages.eco.removed_admin"),
    MONEY_REMOVED("messages.eco.removed_target"),

    ECO_HISTORY_USAGE("messages.eco.history.usage"),
    ECO_HISTORY_FETCHING("messages.eco.history.fetching"),
    ECO_HISTORY_HEADER("messages.eco.history.header"),
    ECO_HISTORY_ENTRY("messages.eco.history.entry"),
    ECO_HISTORY_NO_ENTRIES("messages.eco.history.no_entries"),

    VOUCHER_USAGE("messages.voucher.usage"),
    VOUCHER_CREATED("messages.voucher.created"),
    VOUCHER_CHECKED("messages.voucher.checked"),

    BALTOP_HEADER("messages.baltop.header"),
    BALTOP_ENTRY("messages.baltop.entry"),
    BALTOP_FOOTER("messages.baltop.footer"),
    BALTOP_REFRESHED("messages.baltop.refreshed"),
    BALTOP_INVALID_PAGE("messages.baltop.invalid_page"),

    UPDATE_AVAILABLE("messages.updates.available"),
    UPDATE_LATEST("messages.updates.latest"),

    ACTION_BAR_DETRACT("messages.action_bar.detract"),
    ACTION_BAR_ADD("messages.action_bar.add");

    private final String path;

    LanguageKeys(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }
}