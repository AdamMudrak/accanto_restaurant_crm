package com.example.restaurant.service.utils;

public class PropertyReader {
    private static final int MILLISECONDS_IN_SECOND = 1000;
    private static final int SECONDS_IN_MINUTE = 60;
    private static final int MINUTES_IN_HOUR = 60;
    private static final int HOURS_IN_DAY = 24;

    public static Long getTimeFromPropertiesMillis(String property) {
        long time = Long.parseLong(property.substring(0, property.length() - 1));
        char timeUnit = property.charAt(property.length() - 1);

        return switch (timeUnit) {
            case 's' -> time * MILLISECONDS_IN_SECOND;
            case 'm' -> time * MILLISECONDS_IN_SECOND * SECONDS_IN_MINUTE;
            case 'h' -> time * MILLISECONDS_IN_SECOND * SECONDS_IN_MINUTE * MINUTES_IN_HOUR;
            default -> time * MILLISECONDS_IN_SECOND * SECONDS_IN_MINUTE
                    * MINUTES_IN_HOUR * HOURS_IN_DAY;
        };
    }

    public static Long getTimeFromPropertiesSeconds(String property) {
        return getTimeFromPropertiesMillis(property) / MILLISECONDS_IN_SECOND;
    }
}
