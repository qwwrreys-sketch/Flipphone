package com.flipphone;

/** The few settings of the phone. Change a value and rebuild. */
public final class PhoneConfig {
    private PhoneConfig() {}

    /**
     * false = the keybind (default: Alt) opens the phone for everybody.
     * true  = the keybind only works while a Flip Phone item is held in a hand.
     * Right-clicking the item always opens the phone.
     */
    public static final boolean KEYBIND_NEEDS_ITEM = false;

    // ---- the roleplay calendar shown on the home screen (same idea as a school-year calendar)
    public static final int CALENDAR_START_YEAR = 2009;
    public static final int CALENDAR_START_MONTH = 9;   // 1 = January ... 12 = December
    public static final int DAYS_PER_MONTH = 10;        // one Minecraft day = one RP day
    /** true = after August 2010 the calendar starts again at September 2009 (the year never leaves 2009-2010). */
    public static final boolean CALENDAR_LOOPS = true;
}
