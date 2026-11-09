package com.flipphone.client.ui;

/**
 * Every colour of the phone, sampled from the reference video (ARGB).
 * Screen = pale green-grey LCD, dark status / soft-key bars, LINE green, blue chat background.
 */
public final class PhoneTheme {
    private PhoneTheme() {}

    // ---- LCD
    public static final int LCD = 0xFFE0E5DB;
    public static final int HEADER = 0xFFF2F5E9;        // cream title bar ("MENU", "GAMES" ...)
    public static final int INK = 0xFF191B18;
    public static final int INK_SOFT = 0xFF28281F;
    public static final int MUTED = 0xFF696E64;
    public static final int STATUS_BAR = 0xFF2E2E2B;
    public static final int SOFT_BAR = 0xFF191B18;
    public static final int SOFT_TEXT = 0xFFE8EBE0;
    public static final int TILE = 0xFFA0AB9D;
    public static final int TILE_BORDER = 0xFFACB1A7;
    public static final int TILE_SEL = 0xFF20221F;
    public static final int PANEL = 0xFF9FA798;
    public static final int PANEL_DARK = 0xFF242621;
    public static final int PANEL_LINE = 0xFFDFE4DA;

    // ---- LINE
    public static final int LINE_GREEN = 0xFF06C755;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int ROW_SEL = 0xFFE8EEF4;
    public static final int ROW_LINE = 0xFFE3E7EC;
    public static final int AVATAR = 0xFFFFCB91;
    public static final int AVATAR_ICON = 0xFFE8832A;
    public static final int NAME = 0xFF1F1F1F;
    public static final int GREY = 0xFF8A8F96;
    public static final int CHAT_BG = 0xFF89A9D0;
    public static final int BUBBLE_MINE = 0xFF2FA465;
    public static final int PILL = 0xFF728FB2;
    public static final int INPUT_BG = 0xFFE7EDF3;

    // ---- Snake
    public static final int SNAKE_BG = 0xFFC8D0C3;
    public static final int SNAKE_GRID = 0xFFD2D9CD;
    public static final int SNAKE_HEAD = 0xFF12120F;
    public static final int SNAKE_BODY = 0xFF4C4C47;
    public static final int SNAKE_FOOD = 0xFFDF3322;

    // ---- phone body
    public static final int METAL_TOP = 0xFF8A8A90;
    public static final int METAL_BOTTOM = 0xFF2A2A2E;
    public static final int BODY_EDGE = 0xFF141416;
    public static final int BEZEL = 0xFF060607;
    public static final int PAD_TOP = 0xFF34343A;
    public static final int PAD_BOTTOM = 0xFF0D0D0F;
    public static final int PAD_PANEL = 0xFF09090B;
    public static final int KEY = 0xFF17171B;
    public static final int KEY_EDGE = 0xFF26262B;
    public static final int KEY_HOVER = 0xFF25252B;
    public static final int KEY_PRESS = 0xFF3F3F48;
    public static final int KEY_TEXT = 0xFFC9C9CE;
    public static final int KEY_SUB = 0xFF76767E;

    // ---- shared accents
    public static final int GREEN_BTN = 0xFF1FA85C;
    public static final int RED = 0xFFC8372A;
}
