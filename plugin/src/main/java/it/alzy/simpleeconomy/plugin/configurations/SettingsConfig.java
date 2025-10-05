package it.alzy.simpleeconomy.plugin.configurations;

import java.util.List;

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

    public boolean areVoucherEnabled() {
        return this.fileConfiguration.getBoolean("settings.enable-vouchers", true);
    }

    public String getVoucherMaterial() {
        return this.fileConfiguration.getString("vouchers.item", "PAPER");
    }
    
    public String getVoucherItemName() {
        return this.fileConfiguration.getString("vouchers.name", "PAPER");
    }

    public List<String> getVoucherLore() {
        return this.fileConfiguration.getStringList("vouchers.lore");
    }

    public boolean checkForUpdates() {
        return this.fileConfiguration.getBoolean("settings.check-for-updates", true);
    }

    public int getMaxVoucherAmount() {
        return this.fileConfiguration.getInt("max-check-amount", 1000000);
    }
    
    public String getDBHost() {
        return this.fileConfiguration.getString("database.host", "localhost");
    }
    
    public String getDBUsername() {
        return this.fileConfiguration.getString("database.username", "root");
    }

    public String getDBPassword() {
        return this.fileConfiguration.getString("database.password", "CHANGEME");
    }
    
    public String getDBName() {
        return this.fileConfiguration.getString("database.database", "simpleeconomy");
    }
    
    public int getDBPort() {
        return this.fileConfiguration.getInt("database.port",3306);
    }

    public int getDBMaxPool() {
        return this.fileConfiguration.getInt("database.max-connection-pool", 10);
    }

    public int getBalTopInterval() {
        return this.fileConfiguration.getInt("settings.baltop-update-interval", 20);
    }

    public int getTopPlayersShown() {
        return this.fileConfiguration.getInt("settings.baltop-limit", 10);
    }

    public boolean registerPlaceholderAPI() {
        return this.fileConfiguration.getBoolean("settings.use-placeholderapi", true);
    }

    public String getDBPrefixTable() {
        return this.fileConfiguration.getString("database.table-prefix", "se_");
    }

    private SettingsConfig() {}


    public static SettingsConfig getInstance() {
        if(instance == null) instance = new SettingsConfig();
        return instance;
    }

}
