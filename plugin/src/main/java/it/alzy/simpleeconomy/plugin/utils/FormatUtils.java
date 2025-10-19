package it.alzy.simpleeconomy.plugin.utils;

import java.util.NavigableMap;
import java.util.TreeMap;

import it.alzy.simpleeconomy.plugin.configurations.SettingsConfig;

public class FormatUtils {

    private static final NavigableMap<Double, String> SUFFIXES = new TreeMap<>();

    static {
        SettingsConfig config = SettingsConfig.getInstance();
        SUFFIXES.put(1_000D, config.getThousandsSuffix());
        SUFFIXES.put(1_000_000D, config.getMillionSuffix());
        SUFFIXES.put(1_000_000_000D, config.getBillionSuffix());
        SUFFIXES.put(1_000_000_000_000D, config.getTrillionSuffix());
    }

    public String formatBalance(double balance) {
        SettingsConfig config = SettingsConfig.getInstance();
        String currencySymbol = config.currencySymbol();

        if(!(config.isFormattingEnabled())) return formatDecimal(balance);

        if (balance < 1_000) {
            return currencySymbol + formatDecimal(balance);
        }

        var entry = SUFFIXES.floorEntry(balance);
        if (entry == null) {
            return currencySymbol + formatDecimal(balance);
        }

        double divisor = entry.getKey();
        String suffix = entry.getValue();

        double shortNumber = balance / divisor;

        return currencySymbol + formatDecimal(shortNumber) + suffix;
    }

    private String formatDecimal(double value) {
        if (value == Math.floor(value)) {
            return String.format("%.0f", value);
        } else {
            return String.format("%.1f", value);
        }
    }
}
