package it.alzy.simpleeconomy.plugin.configurations;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import net.pino.simpleconfig.LightConfig;
import net.pino.simpleconfig.annotations.Config;
import net.pino.simpleconfig.annotations.ConfigFile;
import org.bukkit.configuration.file.YamlConfiguration;

@Config
@ConfigFile("config.yml")
public class SettingsConfig extends LightConfig {

    private static SettingsConfig instance = null;

    public String locale() { return this.fileConfiguration.getString("settings.locale", "en"); }

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
        return this.fileConfiguration.getString("vouchers.name", "&cCHECK &7(%playerName%)");
    }

    public List<String> getVoucherLore() {
        return this.fileConfiguration.getStringList("vouchers.lore");
    }

    public boolean checkForUpdates() {
        return this.fileConfiguration.getBoolean("settings.check-for-updates", true);
    }

    public int getMaxVoucherAmount() {
        return this.fileConfiguration.getInt("vouchers.max-check-amount", 1000000);
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
        return this.fileConfiguration.getInt("database.port", 3306);
    }

    public int getDBMaxPool() {
        return this.fileConfiguration.getInt("database.max-connection-pool", 10);
    }

    public int getBalTopInterval() {
        return this.fileConfiguration.getInt("settings.baltop-refresh-interval", 20);
    }

    public int getUpdateCheckInterval() {
        return this.fileConfiguration.getInt("settings.update-check-interval", 1440);
    }

    public int getTopPlayersShown() {
        return this.fileConfiguration.getInt("settings.baltop-limit", 10);
    }

    public boolean isFormattingEnabled() { return this.fileConfiguration.getBoolean("settings.enable-balance-formatting", true); }

    public boolean registerPlaceholderAPI() { return this.fileConfiguration.getBoolean("settings.use-placeholderapi", true); }

    public String getDBPrefixTable() {
        return this.fileConfiguration.getString("database.table-prefix", "se_");
    }

    public boolean isTransactionLoggingEnabled() { return this.fileConfiguration.getBoolean("transaction-logger.enable-logger", true); }

    public int getTransactionLoggingMaxFileSize() { return this.fileConfiguration.getInt("transaction-logger.file-size-limit-mb", 10); }

    public int getMaxTransactionLimit() {
        return this.fileConfiguration.getInt("settings.max-transaction-amount", 0);
    }

    public int getPurgeInactiveAccountsDays() {
        return this.fileConfiguration.getInt("storage.auto-purge-days", 30);
    }

    public boolean isAutoPurgeEnabled() {
        return this.fileConfiguration.getBoolean("storage.enable-auto-purge", true);
    }

    public boolean enableActionBarMessages() {return this.fileConfiguration.getBoolean("settings.enable-action-bar-messages", false);}

    public boolean isInterestEnabled() {return this.fileConfiguration.getBoolean("interests.enabled", false);}

    public double minBalanceForInterests() {return this.fileConfiguration.getDouble("interests.min-balance", 100);}

    public double maxInterest() {return this.fileConfiguration.getDouble("interests.max-interest", 2000);}

    public double getInterestRate() { return this.fileConfiguration.getDouble("interests.rate", 0.05);}

    public int getInterestInterval() { return this.fileConfiguration.getInt("interests.interval", 60); }

    // --- Discord Webhook Getters ---

    public boolean shouldLogToDiscord() { return this.fileConfiguration.getBoolean("settings.enable-discord-logging", false);}

    public String webhookURL() { return this.fileConfiguration.getString("webhook-settings.url", ""); }

    public String username() { return this.fileConfiguration.getString("webhook-settings.username", "SimpleEconomy Logger"); }

    public String avatarURL() { return this.fileConfiguration.getString("webhook-settings.avatar-url", "https://example.com/avatar.png"); }

    public String webhookColor() { return this.fileConfiguration.getString("webhook-settings.color", "#22C55E"); }

    public boolean logPayToDiscord() { return this.fileConfiguration.getBoolean("webhook-settings.log-payments", true); }

    public boolean logWithdrawalsToDiscord() { return this.fileConfiguration.getBoolean("webhook-settings.log-withdrawals", true); }

    public boolean logAdminToDiscord() { return this.fileConfiguration.getBoolean("webhook-settings.log-admin", true); }

    public boolean logVoucherCreations() { return this.fileConfiguration.getBoolean("webhook-settings.log-voucher-creations", true); }


    public void checkMissingKeys() {
        InputStream resourceStream = SimpleEconomy.getInstance().getResource("config.yml");
        if (resourceStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(resourceStream, StandardCharsets.UTF_8));
            boolean changed = false;
            for (String key : defaultConfig.getKeys(true)) {
                if (!this.fileConfiguration.contains(key)) {
                    fileConfiguration.set(key, defaultConfig.get(key));
                    fileConfiguration.setComments(key, defaultConfig.getComments(key));
                    fileConfiguration.setInlineComments(key, defaultConfig.getInlineComments(key));
                    changed = true;
                }
            }
            if (changed) {
                this.saveAndReload();
            }
        }
    }

    private SettingsConfig() {}

    public static SettingsConfig getInstance() {
        if(instance == null) instance = new SettingsConfig();
        return instance;
    }
}