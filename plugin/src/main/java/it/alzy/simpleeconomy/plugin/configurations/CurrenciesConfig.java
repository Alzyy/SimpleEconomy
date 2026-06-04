package it.alzy.simpleeconomy.plugin.configurations;

import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import net.pino.simpleconfig.LightConfig;
import net.pino.simpleconfig.annotations.Config;
import net.pino.simpleconfig.annotations.ConfigFile;

@Config
@ConfigFile("currencies.yml")
public class CurrenciesConfig extends LightConfig {

    private static CurrenciesConfig instance;


    public static CurrenciesConfig getInstance() {
        if (instance == null) {
            instance = new CurrenciesConfig();
        }
        return instance;
    }

    public void removeCurrency(String lowerCase) {
        SimpleEconomy.getInstance().getExecutor().execute(() -> {
            if (fileConfiguration.getString(lowerCase + ".name") != null) {
                fileConfiguration.set(lowerCase, null);
                saveAndReload();
            }
        });
    }
}
