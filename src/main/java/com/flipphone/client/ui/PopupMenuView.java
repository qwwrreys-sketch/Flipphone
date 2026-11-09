package com.flipphone.client.ui;

import java.util.List;
import java.util.function.IntConsumer;

/** A small white popup list drawn over the screen below it (video: Call / Message / Rename / Delete). */
public final class PopupMenuView extends View {
    private final List<String> options;
    private final IntConsumer onPick;
    private int sel;

    public PopupMenuView(PhoneScreen ph, List<String> options, IntConsumer onPick) {
        super(ph);
        this.options = options;
        this.onPick = onPick;
    }

    @Override public String left() { return "BACK"; }
    @Override public String center() { return "SELECT"; }
    @Override public String right() { return ""; }
    @Override public boolean isOverlay() { return true; }

    private int boxY() {
        return 324 - 16 - options.size() * 34;
    }

    @Override
    public void render(PhoneGfx g, int mx, int my) {
        g.fill(0, 0, 266, 324, 0x66000000);
        int y0 = boxY();
        g.rrect(20, y0 - 4, 226, options.size() * 34 + 8, 10, PhoneTheme.WHITE);
        for (int i = 0; i < options.size(); i++) {
            int y = y0 + i * 34;
            if (i == sel) g.rrect(24, y, 218, 32, 7, PhoneTheme.ROW_SEL);
            boolean danger = options.get(i).equals("Delete");
            g.text(options.get(i), 36, y + 11, 11.5f, danger ? PhoneTheme.RED : PhoneTheme.NAME);
        }
    }

    private void pick() {
        ph.pop();
        onPick.accept(sel);
    }

    @Override
    public boolean onKey(Key k) {
        switch (k) {
            case UP: if (sel > 0) sel--; return true;
            case DOWN: if (sel < options.size() - 1) sel++; return true;
            case OK: pick(); return true;
            case MENU: return true;
            default: return super.onKey(k);
        }
    }

    @Override
    public boolean onClick(int x, int y) {
        int y0 = boxY();
        if (x >= 24 && x < 242 && y >= y0 && y < y0 + options.size() * 34) {
            sel = (y - y0) / 34;
            pick();
            return true;
        }
        ph.pop(); // clicking outside closes the popup
        return true;
    }
}
