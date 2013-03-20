package de.akesting.utils;

import java.util.Formatter;

public final class FormatUtils {

    private static Formatter formatter;

    private FormatUtils() {
        // private constructor
    }

    // TODO refactor with JodaTime
    public static String getFormatedTime(double timeInSeconds) {
        int intTime = (int) timeInSeconds;
        int hours = intTime / 3600;
        intTime = intTime % 3600;
        int min = intTime / 60;
        intTime = intTime % 60;
        StringBuilder stringBuilder = new StringBuilder();
        formatter = new Formatter(stringBuilder);
        formatter.format("%02d:%02d:%02d", hours, min, intTime);
        return stringBuilder.toString();
    }

}
