package org.zzq.pathSelection.util;

public class TimeFormatter {

    public static String formatMilliseconds(long milliseconds) {
        long minutes = milliseconds / 60000;
        long seconds = (milliseconds % 60000) / 1000;
        long millis = milliseconds % 1000;

        if (minutes > 0) {
            return String.format("%d分%02d秒%03d毫秒", minutes, seconds, millis);
        } else {
            return String.format("%d秒%03d毫秒", seconds, millis);
        }
    }

    public static String formatMillisecondsSimple(long milliseconds) {
        long seconds = milliseconds / 1000;
        long millis = milliseconds % 1000;
        return String.format("%d.%03d", seconds, millis);
    }

    public static String getCurrentDate() {
        return java.time.LocalDate.now().toString(); // yyyy-MM-dd
    }
}
