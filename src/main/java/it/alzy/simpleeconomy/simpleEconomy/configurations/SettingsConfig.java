package it.alzy.simpleeconomy.simpleEconomy.configurations;

import net.pino.simpleconfig.LightConfig;
import net.pino.simpleconfig.annotations.Config;
import net.pino.simpleconfig.annotations.ConfigFile;

@Config
@ConfigFile("config.yml")
public class SettingsConfig extends LightConfig {
    private static SettingsConfig instance = null;

    public double startingBalance() {
        return this.fileConfiguration.getDouble("settings.starting-balance", 1000d);
    }

    public String currencySymbol() {
        return this.fileConfiguration.getString("settings.currency-symbol", "$");
    }

    public String storageSystem() {
        return this.fileConfiguration.getString("settings.storage-system", "SQLITE");
    }

    public int autoSaveInterval() {
        return this.fileConfiguration.getInt("settings.auto-save-time", 30);
    }

    public int getThreadPoolSize() {
        return this.fileConfiguration.getInt("settings.max-thread-pool-size", 2);
    }

    public String getThousandsSuffix() {
        return this.fileConfiguration.getString("format.thousand", "k");
    }

    public String getMillionSuffix() {
        return this.fileConfiguration.getString("format.million", "M");
    }

    public String getBillionSuffix() {
        return this.fileConfiguration.getString("format.billion", "B");
    }

    public String getTrillionSuffix() {
        return this.fileConfiguration.getString("format.trillion", "T");
    }

    private SettingsConfig() {}


    public static SettingsConfig getInstance() {
        if(instance == null) instance = new SettingsConfig();
        return instance;
    }

}
