package de.akesting.utils;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;


public final class FormatUtils {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("YYYY-MM-dd'T'HH:mm:ss");
    private static final double SECONDS_TO_MILLIS = 1000;

    private FormatUtils() {
        // private constructor
    }

    public static String getFormattedTime(double timeInSeconds) {
        long instant = (long) (timeInSeconds * SECONDS_TO_MILLIS);
        DateTime dateTime = new DateTime(instant, DateTimeZone.UTC);
        return DATE_TIME_FORMATTER.print(dateTime);
    }

}
