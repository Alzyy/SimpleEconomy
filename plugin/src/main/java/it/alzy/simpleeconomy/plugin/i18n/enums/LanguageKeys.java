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
    ACTION_BAR_ADD("messages.action_bar.add"),

    INTERESTS_RECEIVED("messages.interests.received"),

    CURRENCY_ADD("messages.currencies.add"),
    CURRENCY_REMOVE("messages.currencies.remove"),
    CURRENCY_LIST("messages.currencies.list"),
    CURRENCY_ALREADY_EXISTS("messages.currencies.already-exists"),
    CURRENCY_NOT_FOUND("messages.currencies.not-found"),
    CURRENCY_LIST_EMPTY("messages.currencies.list-currency-empty"),
    CURRENCY_SYMBOL_CHANGED("messages.currencies.symbol-changed"),

    MODULES_LIST("messages.modules.list"),
    MODULES_NO_MODULES("messages.modules.no_modules"),
    MODULES_STATUS("messages.modules.status"),
    MODULE_DISABLED("messages.modules.disabled"),
    MODULE_NOT_FOUND("messages.modules.not_found"),
    MODULE_LOADED_FROM_FILE("messages.modules.loaded_from_file"),
    MODULE_COULDNT_FIND_FILE("messages.modules.couldnt_find_file"),
    MODULE_ENABLED_PLACEHOLDER("messages.modules.enabled_placeholder"),
    MODULE_DISABLED_PLACEHOLDER("messages.modules.disabled_placeholder"),
    DIAGNOSE_RESULT("messages.system.diagnose_result");


    private final String path;

    LanguageKeys(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }
}