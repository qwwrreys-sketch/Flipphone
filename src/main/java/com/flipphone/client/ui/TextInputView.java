package com.flipphone.client.ui;

import java.util.function.Consumer;

/** A one-line text prompt (nickname, rename). */
public final class TextInputView extends View {
    private final String title;
    private final String subtitle;
    private final int maxLen;
    private final Consumer<String> onSubmit;
    private final StringBuilder text;

    public TextInputView(PhoneScreen ph, String title, String subtitle, String initial, int maxLen, Consumer<String> onSubmit) {
        super(ph);
        this.title = title;
        this.subtitle = subtitle;
        this.maxLen = maxLen;
        this.onSubmit = onSubmit;
        this.text = new StringBuilder(initial);
    }

    @Override public String left() { return "BACK"; }
    @Override public String center() { return "SAVE"; }
    @Override public String right() { return "DEL"; }
    @Override public boolean textMode() { return true; }

    @Override
    public void render(PhoneGfx g, int mx, int my) {
        Widgets.header(g, title);
        g.textC(subtitle, 133, 44, 11f, PhoneTheme.INK);
        g.textC("Type a name (max " + maxLen + " letters)", 133, 64, 8f, PhoneTheme.MUTED);
        Widgets.inputBox(g, 13, 90, 240, 38, text.toString(), "Name", 13f, true);
        Widgets.button(g, 33, 270, 200, 34, "Save", PhoneTheme.INK, PhoneTheme.SOFT_TEXT);
    }

    private void submit() {
        String t = text.toString().trim();
        if (t.isEmpty()) return;
        ph.pop();
        onSubmit.accept(t);
    }

    @Override
    public boolean onKey(Key k) {
        switch (k) {
            case OK: submit(); return true;
            case RIGHT_SOFT:
                if (text.length() > 0) text.setLength(text.length() - 1);
                return true;
            case BACK:
                if (text.length() > 0) text.setLength(text.length() - 1);
                else ph.pop();
                return true;
            default: return super.onKey(k);
        }
    }

    @Override
    public void onChar(char c) {
        if (text.length() < maxLen && c != '\u00a7' && !Character.isISOControl(c)) text.append(c);
    }

    @Override
    public boolean onClick(int x, int y) {
        if (x >= 33 && x < 233 && y >= 270 && y < 304) {
            submit();
            return true;
        }
        return false;
    }
}
