package com.flipphone.client.ui;

import com.flipphone.PhoneConfig;

import net.minecraft.client.Minecraft;

/** The time and date on the home screen, taken from the Minecraft world clock (so it matches the sky). */
final class GameClock {
    private GameClock() {}

    record Now(String clock, String ampm, String date) {}

    private static final String[] MONTHS = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};

    static Now now() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return new Now("10.30", "AM", "MAR 2007");
        long dayTime = mc.level.getDayTime();
        // Minecraft: dayTime 0 = 06:00, 6000 = noon, 18000 = midnight
        long t = Math.floorMod(dayTime, 24000L);
        int hour = (int) ((t / 1000 + 6) % 24);
        int minute = (int) ((t % 1000) * 60 / 1000);
        int h12 = hour % 12 == 0 ? 12 : hour % 12;

        // the calendar day changes at midnight
        long dayIndex = Math.floorDiv(dayTime + 6000L, 24000L);
        long monthIndex = Math.floorDiv(dayIndex, (long) PhoneConfig.DAYS_PER_MONTH);
        if (PhoneConfig.CALENDAR_LOOPS) monthIndex = Math.floorMod(monthIndex, 12L);
        long absMonth = (PhoneConfig.CALENDAR_START_MONTH - 1) + monthIndex;
        int month = (int) Math.floorMod(absMonth, 12L);
        long year = PhoneConfig.CALENDAR_START_YEAR + Math.floorDiv(absMonth, 12L);

        return new Now(h12 + "." + (minute < 10 ? "0" : "") + minute, hour < 12 ? "AM" : "PM", MONTHS[month] + " " + year);
    }
}
