package it.alzy.simpleeconomy.plugin.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.i18n.enums.LanguageKeys;
import org.bukkit.entity.Player;

import java.util.stream.Collectors;

@CommandAlias("currencies|currs|curs")
@CommandPermission("simpleconomy.command.currencies")
@Description("Manage your multi currencies with this command")
public class CurrenciesCommand extends BaseCommand {

    private final SimpleEconomy plugin = SimpleEconomy.getInstance();

    @Subcommand("create|add")
    @Syntax("<currencyName> <symbol>")
    public void createCurrency(Player player, String name, String symbol) {
        if (!player.hasPermission("simpleconomy.command.currencies.manage")) {
            plugin.getLanguageManager().send(player, LanguageKeys.NO_PERMISSION, "%prefix%", plugin.getLanguageManager().getMessage(LanguageKeys.PREFIX));
            return;
        }
        if (plugin.getCurrencyManager().getCurrency(name) != null) {
            plugin.getLanguageManager().send(player, LanguageKeys.CURRENCY_ALREADY_EXISTS, "%prefix%", plugin.getLanguageManager().getMessage(LanguageKeys.PREFIX), "%currency%", name);
            return;
        }
        plugin.getCurrencyManager().registerCurrency(name, symbol);
        plugin.getLanguageManager().send(player, LanguageKeys.CURRENCY_ADD, "%prefix%", plugin.getLanguageManager().getMessage(LanguageKeys.PREFIX), "%currency%", name);
    }

    @Subcommand("delete|remove")
    @CommandCompletion("@currencies")
    @Syntax("<currencyName>")
    public void deleteCurrency(Player player, String name) {
        if (!player.hasPermission("simpleconomy.command.currencies.manage")) {
            plugin.getLanguageManager().send(player, LanguageKeys.NO_PERMISSION, "%prefix%", plugin.getLanguageManager().getMessage(LanguageKeys.PREFIX));
            return;
        }
        if (plugin.getCurrencyManager().getCurrency(name) == null) {
            plugin.getLanguageManager().send(player, LanguageKeys.CURRENCY_NOT_FOUND, "%prefix%", plugin.getLanguageManager().getMessage(LanguageKeys.PREFIX), "%currency%", name);
            return;
        }
        plugin.getCurrencyManager().unregisterCurrency(name);
        plugin.getLanguageManager().send(player, LanguageKeys.CURRENCY_REMOVE, "%prefix%", plugin.getLanguageManager().getMessage(LanguageKeys.PREFIX), "%currency%", name);
    }

    @Subcommand("symbol")
    @CommandCompletion("@currencies @nothing")
    @Syntax("<currencyName> <newSymbol>")
    public void changeSymbol(Player player, String name, String newSymbol) {
        if (!player.hasPermission("simpleconomy.command.currencies.manage")) {
            plugin.getLanguageManager().send(player, LanguageKeys.NO_PERMISSION, "%prefix%", plugin.getLanguageManager().getMessage(LanguageKeys.PREFIX));
            return;
        }
        var currency = plugin.getCurrencyManager().getCurrency(name);
        if (currency == null) {
            plugin.getLanguageManager().send(player, LanguageKeys.CURRENCY_NOT_FOUND, "%prefix%", plugin.getLanguageManager().getMessage(LanguageKeys.PREFIX), "%currency%", name);
            return;
        }
        String oldSymbol = currency.getSymbol();
        if (newSymbol == null || newSymbol.isEmpty()) {
            newSymbol = "";
        }
        plugin.getCurrencyManager().changeSymbol(name, newSymbol);
        plugin.getLanguageManager().send(player, LanguageKeys.CURRENCY_SYMBOL_CHANGED, "%prefix%", plugin.getLanguageManager().getMessage(LanguageKeys.PREFIX), "%currency%", name, "%symbol%", newSymbol, "%oldSymbol%", oldSymbol);
    }

    @Subcommand("list")
    public void listCurrencies(Player player) {
        if (!player.hasPermission("simpleconomy.command.currencies.manage")) {
            plugin.getLanguageManager().send(player, LanguageKeys.NO_PERMISSION, "%prefix%", plugin.getLanguageManager().getMessage(LanguageKeys.PREFIX));
            return;
        }

        var currencies = plugin.getCurrencyManager().getAllCurrencies();
        if (currencies.isEmpty()) {
            plugin.getLanguageManager().send(player, LanguageKeys.CURRENCY_LIST_EMPTY, "%prefix%", plugin.getLanguageManager().getMessage(LanguageKeys.PREFIX));
            return;
        }

        String formattedList = currencies.stream()
                .map(currency -> currency.getName() + " (" + currency.getSymbol() + ")")
                .collect(Collectors.joining(", "));

        plugin.getLanguageManager().send(player, LanguageKeys.CURRENCY_LIST, "%prefix%", plugin.getLanguageManager().getMessage(LanguageKeys.PREFIX), "%currencies%", formattedList);
    }
}