package com.flipphone.client.ui;

/** The app grid (video: "MENU"): 3 columns of tiles, the selected one turns dark. */
public final class MenuView extends View {
    enum App {
        LINE("LINE"), DIAL("DIAL"), CONTACTS("CONTACTS"), CALLS("CALLS"), GAMES("GAMES"), MY_PHONE("MY PHONE"),
        SOCIALLINK("SOCIALLINK"), YENAPP("YENAPP");

        final String label;

        App(String label) {
            this.label = label;
        }
    }

    private static final App[] APPS = App.values();
    private int sel;

    public MenuView(PhoneScreen ph) {
        super(ph);
    }

    @Override public String left() { return "BACK"; }
    @Override public String center() { return "OK"; }
    @Override public String right() { return ""; }

    private static int tileX(int i) {
        return 7 + 85 * (i % 3);
    }

    private static int tileY(int i) {
        return 32 + 47 * (i / 3);
    }

    @Override
    public void render(PhoneGfx g, int mx, int my) {
        Widgets.header(g, "MENU");
        for (int i = 0; i < APPS.length; i++) {
            int x = tileX(i);
            int y = tileY(i);
            boolean on = i == sel;
            g.rrect(x, y, 80, 44, 7, on ? PhoneTheme.TILE_SEL : PhoneTheme.TILE_BORDER);
            g.rrect(x + 1, y + 1, 78, 42, 6, on ? PhoneTheme.TILE_SEL : PhoneTheme.TILE);
            icon(g, APPS[i], x + 28, y + 5);
            g.textC(APPS[i].label, x + 40, y + 32, 7.5f, on ? PhoneTheme.SOFT_TEXT : PhoneTheme.INK_SOFT);
        }
    }

    /** 24 x 24 unit icons made of shapes and symbol characters. */
    private static void icon(PhoneGfx g, App app, int x, int y) {
        switch (app) {
            case LINE:
                g.rrect(x, y, 24, 24, 6, PhoneTheme.LINE_GREEN);
                g.rrect(x + 4, y + 5, 16, 11, 5, PhoneTheme.WHITE);
                g.fill(x + 8, y + 14, 4, 5, PhoneTheme.WHITE);
                g.fill(x + 8, y + 9, 2, 2, PhoneTheme.LINE_GREEN);
                g.fill(x + 11, y + 9, 2, 2, PhoneTheme.LINE_GREEN);
                g.fill(x + 14, y + 9, 2, 2, PhoneTheme.LINE_GREEN);
                break;
            case YENAPP:
                g.rrect(x, y, 24, 24, 6, PhoneTheme.GREEN_BTN);
                g.textC("\u00a5", x + 12, y + 3, 18f, PhoneTheme.WHITE);
                break;
            case SOCIALLINK:
                g.rrect(x, y, 24, 24, 6, 0xFF4A9FD8);
                g.textC("\u21c4", x + 12, y + 3, 17f, PhoneTheme.WHITE);
                break;
            case DIAL:
                g.textC("\u260e", x + 12, y + 2, 20f, PhoneTheme.INK_SOFT);
                break;
            case CONTACTS:
                g.rrect(x + 3, y + 1, 18, 22, 3, PhoneTheme.INK_SOFT);
                g.circle(x + 12, y + 9, 4, PhoneTheme.LCD);
                g.rrect(x + 6, y + 14, 12, 6, 3, PhoneTheme.LCD);
                break;
            case CALLS:
                g.textC("\u21bb", x + 12, y + 2, 20f, PhoneTheme.INK_SOFT);
                break;
            case GAMES:
                g.rrect(x + 1, y + 6, 22, 13, 6, PhoneTheme.INK_SOFT);
                g.fill(x + 6, y + 11, 6, 2, PhoneTheme.LCD);
                g.fill(x + 8, y + 9, 2, 6, PhoneTheme.LCD);
                g.circle(x + 17, y + 11, 2, PhoneTheme.LCD);
                break;
            default: // MY_PHONE
                g.circle(x + 12, y + 12, 11, PhoneTheme.INK_SOFT);
                g.circle(x + 12, y + 12, 9, PhoneTheme.TILE);
                g.textC("i", x + 12, y + 5, 14f, PhoneTheme.INK_SOFT);
                break;
        }
    }

    static View create(PhoneScreen ph, App app) {
        switch (app) {
            case LINE: return new LineListView(ph);
            case DIAL: return new DialView(ph, "");
            case CONTACTS: return new ContactsView(ph, false);
            case CALLS: return new CallsView(ph);
            case GAMES: return new GamesView(ph);
            case MY_PHONE: return new MyPhoneView(ph);
            case SOCIALLINK: return new SocialLinkView(ph);
            default: return new YenAppView(ph);
        }
    }

    private void launch() {
        ph.push(create(ph, APPS[sel]));
    }

    @Override
    public boolean onKey(Key k) {
        switch (k) {
            case UP: if (sel - 3 >= 0) sel -= 3; return true;
            case DOWN: if (sel + 3 < APPS.length) sel += 3; return true;
            case LEFT: if (sel > 0) sel--; return true;
            case RIGHT: if (sel < APPS.length - 1) sel++; return true;
            case OK: launch(); return true;
            case MENU: return true;
            default: return super.onKey(k);
        }
    }

    @Override
    public boolean onClick(int x, int y) {
        for (int i = 0; i < APPS.length; i++) {
            if (x >= tileX(i) && x < tileX(i) + 80 && y >= tileY(i) && y < tileY(i) + 44) {
                sel = i;
                launch();
                return true;
            }
        }
        return false;
    }
}
