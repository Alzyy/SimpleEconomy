package it.alzy.simpleeconomy.plugin.utils;

import java.time.LocalDateTime;

public class TimeUtils {

    public static String dateTime(long timestamp) {
        LocalDateTime dateTime = LocalDateTime.ofEpochSecond(timestamp / 1000, 0, java.time.ZoneOffset.UTC);
        return dateTime.toString();
    }
}
