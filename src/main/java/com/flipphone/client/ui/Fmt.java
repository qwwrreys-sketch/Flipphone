package com.flipphone.client.ui;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Small text-formatting helpers. */
public final class Fmt {
    private Fmt() {}

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter MD = DateTimeFormatter.ofPattern("MMM dd", Locale.ENGLISH);

    public static String hm(long millis) {
        return HM.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()));
    }

    /** "DEC 06" */
    public static String monthDay(long millis) {
        return MD.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())).toUpperCase(Locale.ENGLISH);
    }

    /** "2m 41s" */
    public static String duration(long seconds) {
        long m = seconds / 60;
        long s = seconds % 60;
        return m + "m " + s + "s";
    }

    /** "6m31s" (compact, for the My Phone screen) */
    public static String durationCompact(long seconds) {
        return (seconds / 60) + "m" + String.format("%02d", seconds % 60) + "s";
    }

    /** "01:23" */
    public static String clock(long seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    public static String yen(long amount) {
        return "\u00a5" + String.format(Locale.ENGLISH, "%,d", amount);
    }

    /** "09012" -> "090-12", "09012345678" -> "090-1234-5678" (formats while typing). */
    public static String number(String digits) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digits.length() && i < 11; i++) {
            if (i == 3 || i == 7) sb.append('-');
            sb.append(digits.charAt(i));
        }
        return sb.toString();
    }
}
