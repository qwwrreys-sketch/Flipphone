package com.flipphone.client.ui;

/** The standby screen: big clock, date, pixel flowers and the MENU button (video: first screen). */
public final class HomeView extends View {
    public HomeView(PhoneScreen ph) {
        super(ph);
    }

    @Override public String left() { return "MSG"; }
    @Override public String center() { return "OK"; }
    @Override public String right() { return "TEL"; }

    @Override
    public void render(PhoneGfx g, int mx, int my) {
        GameClock.Now n = GameClock.now();
        g.textC(n.clock() + " " + n.ampm(), 133, 52, 28f, PhoneTheme.INK);
        g.textC(n.date(), 133, 103, 9.5f, PhoneTheme.MUTED);
        g.fill(22, 125, 222, 2, 0xFF62645D);
        g.textC("\u30c9\u30b3\u30e2", 133, 140, 7.5f, PhoneTheme.MUTED);

        flower(g, 43, 235);
        flower(g, 94, 228);
        flower(g, 145, 235);
        flower(g, 197, 224);
        flower(g, 223, 248);

        g.rrect(51, 285, 166, 32, 7, PhoneTheme.INK);
        g.textC("\u25bc  MENU", 134, 295, 11f, PhoneTheme.SOFT_TEXT);
    }

    /** A little pink pixel flower with a green stem. */
    private static void flower(PhoneGfx g, int cx, int cy) {
        int p = 6;
        int pink = 0xFFF08AA6;
        g.fill(cx - p / 2, cy - p - p / 2, p, p, pink);
        g.fill(cx - p / 2, cy + p / 2, p, p, pink);
        g.fill(cx - p - p / 2, cy - p / 2, p, p, pink);
        g.fill(cx + p / 2, cy - p / 2, p, p, pink);
        g.fill(cx - p / 2, cy - p / 2, p, p, 0xFFF5D36A);
        g.fill(cx - p / 2, cy + p + p / 2, p, p, 0xFF6AA84F);
    }

    @Override
    public boolean onKey(Key k) {
        switch (k) {
            case OK:
                ph.openMenu();
                return true;
            case LEFT_SOFT:
                ph.push(new LineListView(ph));
                return true;
            case RIGHT_SOFT:
                ph.push(new DialView(ph, ""));
                return true;
            default:
                return super.onKey(k);
        }
    }

    @Override
    public void onChar(char c) {
        if (c >= '0' && c <= '9') ph.push(new DialView(ph, String.valueOf(c))); // "0-9 Dial"
    }

    @Override
    public boolean onClick(int x, int y) {
        if (x >= 51 && x < 217 && y >= 285 && y < 317) {
            ph.openMenu();
            return true;
        }
        return false;
    }
}
