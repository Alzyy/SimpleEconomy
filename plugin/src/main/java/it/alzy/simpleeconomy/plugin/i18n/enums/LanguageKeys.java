package it.alzy.simpleeconomy.plugin.i18n.enums;

public enum LanguageKeys {

    PREFIX("prefix"),

    GAVE_MONEY("messages.gave_money"),
    RECEIVED_MONEY("messages.received_money"),
    INVALID_AMOUNT("messages.invalid_amount"),
    NEGATIVE_AMOUNT("messages.negative_amount"),
    RELOAD_SUCCESS("messages.reload_success"),
    NO_PERMISSION("messages.no_permission"),
    PLAYER_NOT_FOUND("messages.player_not_found"),
    BALANCE_CHECK_SELF("messages.balance_check_self"),
    BALANCE_CHECK_OTHER("messages.balance_check_other"),
    NOT_ENOUGH_MONEY("messages.not_enough_money"),
    REMOVED_MONEY("messages.removed_money"),
    MONEY_REMOVED("messages.money_removed"),
    SELF_COMMAND("messages.self-command"),
    INVENTORY_FULL("messages.inventory-full"),
    VOUCHER_CREATED("messages.voucher-created"),
    VOUCHER_CHECKED("messages.voucher-checked"),

    ECO_SET_SUCCESS("messages.eco.set_success"),
    ECO_USAGE("messages.eco.usage"),

    BALTOP_HEADER("messages.baltop.header"),
    BALTOP_ENTRY("messages.baltop.entry"),
    BALTOP_REFRESHED("messages.baltop.refreshed"),
    BALTOP_INVALID_PAGE("messages.baltop.invalid_page"),
    BALTOP_FOOTER("messages.baltop.footer"),

    UPDATE_AVAILABLE("messages.updates.available"),
    UPDATE_LATEST("messages.updates.latest"),

    PAY_USAGE("messages.pay.usage"),

    VOUCHER_USAGE("messages.voucher.usage");

    private final String path;

    LanguageKeys(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }
}