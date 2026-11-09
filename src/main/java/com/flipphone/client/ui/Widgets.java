package com.flipphone.client.ui;

/** Pieces that several views use. */
final class Widgets {
    private Widgets() {}

    static boolean blink() {
        return (System.currentTimeMillis() / 500) % 2 == 0;
    }

    /** The cream title bar at the top of most screens. */
    static void header(PhoneGfx g, String title) {
        g.fill(0, 0, 266, 26, PhoneTheme.HEADER);
        g.fill(0, 25, 266, 1, 0xFFC9CEC0);
        g.text(title, 7, 8, 11f, PhoneTheme.INK_SOFT);
    }

    static void button(PhoneGfx g, int x, int y, int w, int h, String label, int bg, int fg) {
        g.rrect(x, y, w, h, 6, bg);
        g.textC(label, x + w / 2f, y + (h - 11) / 2f, 11f, fg);
    }

    /** A white-ish text box with an optional blinking caret and a grey placeholder. */
    static void inputBox(PhoneGfx g, int x, int y, int w, int h, String text, String placeholder, float size, boolean caret) {
        g.rrect(x, y, w, h, 8, 0xFF2B2D28);
        g.rrect(x + 2, y + 2, w - 4, h - 4, 7, PhoneTheme.HEADER);
        float ty = y + (h - size) / 2f;
        if (text.isEmpty()) {
            g.text(placeholder, x + 8, ty, size, 0xFF9AA095);
            if (caret && blink()) g.fill(x + 8, y + 7, 1, h - 14, PhoneTheme.INK);
        } else {
            String shown = g.fit(text, size, w - 20);
            g.text(shown, x + 8, ty, size, PhoneTheme.INK);
            if (caret && blink()) g.fill((int) (x + 9 + g.width(shown, size)), y + 7, 1, h - 14, PhoneTheme.INK);
        }
    }

    /** The orange LINE-style avatar with a little person. */
    static void avatar(PhoneGfx g, int x, int y, int s) {
        g.rrect(x, y, s, s, 7, PhoneTheme.AVATAR);
        int c = PhoneTheme.AVATAR_ICON;
        g.circle(x + s / 2, y + s * 38 / 100, s / 8 + 1, c);
        g.rrect(x + s / 2 - s / 5, y + s * 56 / 100, s * 2 / 5, s / 4, s / 8, c);
        int in = PhoneTheme.AVATAR;
        g.circle(x + s / 2, y + s * 38 / 100, s / 8 - 1, in);
        g.rrect(x + s / 2 - s / 5 + 2, y + s * 56 / 100 + 2, s * 2 / 5 - 4, s / 4 - 2, s / 9, in);
    }

    /** Dotted "Searching..." style animation text. */
    static String dots() {
        int n = (int) ((System.currentTimeMillis() / 400) % 4);
        return ".".repeat(n);
    }
}
