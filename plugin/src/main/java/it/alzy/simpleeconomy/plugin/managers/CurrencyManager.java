package it.alzy.simpleeconomy.plugin.managers;

import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.configurations.CurrenciesConfig;
import it.alzy.simpleeconomy.plugin.model.DynamicCommand;
import it.alzy.simpleeconomy.plugin.model.VirtualCurrency;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CurrencyManager {

    private final Map<String, VirtualCurrency> currencies = new HashMap<>();
    private final SimpleEconomy plugin;
    private final CurrenciesConfig config = CurrenciesConfig.getInstance();

    public CurrencyManager() {
        this.plugin = SimpleEconomy.getInstance();

        final FileConfiguration yaml = config.fileConfiguration;

        for (String key : yaml.getKeys(false)) {
            String name = yaml.getString(key + ".name");
            String symbol = yaml.getString(key + ".symbol");

            if (name != null && symbol != null) {
                VirtualCurrency vc = new VirtualCurrency(name, symbol);
                currencies.put(name.toLowerCase(), vc);


                plugin.getCommandManager().getCommandReplacements().addReplacement("currencyName", name);
                plugin.getCommandManager().registerCommand(new DynamicCommand(vc));
                plugin.getLanguageManager().setupCurrencyTranslations(vc);
            }
        }
    }

    private void saveToConfig(VirtualCurrency vc) {
        plugin.getExecutor().execute(() -> {
            String path = vc.getColumnOrKey();
            if (config.fileConfiguration.getString(path + ".name") == null) {

                config.fileConfiguration.set(path + ".name", vc.getName());
                config.fileConfiguration.set(path + ".symbol", vc.getSymbol());
                config.saveAndReload();

            }
        });
    }

    public void registerCurrency(String name, String symbol) {
        String lowerName = name.toLowerCase();

        if (!currencies.containsKey(lowerName)) {

            VirtualCurrency vc = new VirtualCurrency(name, symbol);
            currencies.put(lowerName, vc);

            if (config.fileConfiguration.getString(lowerName + ".name") == null) {
                saveToConfig(vc);
            }


            plugin.getCommandManager().getCommandReplacements().addReplacement("currencyName", name);
            plugin.getCommandManager().registerCommand(new DynamicCommand(vc));
            plugin.getLanguageManager().setupCurrencyTranslations(vc);

        }
    }

    public VirtualCurrency getCurrency(String name) {
        return currencies.get(name.toLowerCase());
    }

    public Collection<VirtualCurrency> getAllCurrencies() {
        return currencies.values();
    }

    public void unregisterCurrency(String name) {
        currencies.remove(name.toLowerCase());
        plugin.getLanguageManager().removeCurrencyMessages(name);
        config.removeCurrency(name.toLowerCase());
    }

    public void changeSymbol(String name, String newSymbol) {
        VirtualCurrency vc = getCurrency(name);
        if (vc != null) {
            vc.setSymbol(newSymbol);
            saveToConfig(vc);
        }
    }
}