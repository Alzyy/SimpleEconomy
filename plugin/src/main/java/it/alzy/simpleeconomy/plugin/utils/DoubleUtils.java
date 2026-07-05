package it.alzy.simpleeconomy.plugin.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

import it.alzy.simpleeconomy.plugin.configurations.SettingsConfig;

public class DoubleUtils {


    private final int decimalPlaces;

    public DoubleUtils() {
        this.decimalPlaces = SettingsConfig.getInstance().getDecimalPlaces();
    }

    public double round(double value) {
        if (decimalPlaces < 0) throw new IllegalArgumentException("Decimal places cannot be negative");
        return BigDecimal.valueOf(value).setScale(decimalPlaces, RoundingMode.HALF_UP).doubleValue();
    }
}