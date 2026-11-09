package com.flipphone.client.ui;

/** "GAMES": the game list (video). Only Snake is included. */
public final class GamesView extends View {
    public GamesView(PhoneScreen ph) {
        super(ph);
    }

    @Override public String left() { return "BACK"; }
    @Override public String center() { return "OPEN"; }
    @Override public String right() { return ""; }

    @Override
    public void render(PhoneGfx g, int mx, int my) {
        Widgets.header(g, "GAMES");
        // the SNAKE tile (selected)
        g.rrect(4, 31, 126, 56, 7, PhoneTheme.TILE_SEL);
        g.rrect(51, 38, 32, 32, 7, 0xFFE9EBDD);
        // little snake icon
        g.fill(60, 46, 6, 6, 0xFF3E403A);
        g.fill(60, 52, 6, 6, 0xFF3E403A);
        g.fill(66, 58, 6, 6, 0xFF3E403A);
        g.fill(72, 58, 6, 6, 0xFF3E403A);
        g.fill(72, 52, 3, 3, 0xFFDF3322);
        g.textC("SNAKE", 67, 74, 8f, PhoneTheme.SOFT_TEXT);
    }

    @Override
    public boolean onKey(Key k) {
        if (k == Key.OK) {
            ph.push(new SnakeView(ph));
            return true;
        }
        return super.onKey(k);
    }

    @Override
    public boolean onClick(int x, int y) {
        if (x >= 4 && x < 130 && y >= 31 && y < 87) {
            ph.push(new SnakeView(ph));
            return true;
        }
        return false;
    }
}
