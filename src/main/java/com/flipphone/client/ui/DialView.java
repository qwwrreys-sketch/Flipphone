package com.flipphone.client.ui;

import com.flipphone.client.PhoneClientNet;
import com.flipphone.client.PhoneClientState;

/** "DIAL": type a number with the keypad or keyboard, CALL to ring it (video). */
public final class DialView extends View {
    private final StringBuilder digits = new StringBuilder();
    private boolean onButton; // keyboard focus on the "OPEN SOCIALLINK" button

    public DialView(PhoneScreen ph, String initial) {
        super(ph);
        digits.append(initial);
    }

    @Override public String left() { return "BACK"; }
    @Override public String center() { return "CALL"; }
    @Override public String right() { return "DEL"; }

    @Override
    public void render(PhoneGfx g, int mx, int my) {
        Widgets.header(g, "DIAL");
        Widgets.inputBox(g, 7, 34, 252, 40, Fmt.number(digits.toString()), "", 17f, !onButton);
        g.textC("ENTER NUMBER", 133, 84, 8f, PhoneTheme.MUTED);
        String formatted = Fmt.number(digits.toString());
        if (digits.length() == 11 && PhoneClientState.isContact(formatted)) {
            g.textC("\u2192 " + PhoneClientState.nameOf(formatted), 133, 104, 11f, PhoneTheme.INK);
        }
        g.textC("WANT TO GET CONTACTS FASTER? USE SOCIALLINK", 133, 258, 7f, PhoneTheme.MUTED);
        g.rrect(7, 276, 252, 34, 6, onButton ? PhoneTheme.TILE_SEL : PhoneTheme.INK);
        if (onButton) g.outline(7, 276, 252, 34, 2, PhoneTheme.LINE_GREEN);
        g.textC("OPEN SOCIALLINK", 133, 288, 10f, PhoneTheme.SOFT_TEXT);
    }

    private void call() {
        if (digits.length() != 11) return;
        PhoneClientState.callPhase = PhoneClientState.CALL_NONE;
        PhoneClientNet.dial(Fmt.number(digits.toString()));
        ph.push(new CallView(ph));
    }

    @Override
    public void onChar(char c) {
        if (c >= '0' && c <= '9' && digits.length() < 11) digits.append(c);
    }

    @Override
    public boolean onKey(Key k) {
        switch (k) {
            case OK:
                if (onButton) ph.push(new SocialLinkView(ph));
                else call();
                return true;
            case DOWN: onButton = true; return true;
            case UP: onButton = false; return true;
            case RIGHT_SOFT:
                if (digits.length() > 0) digits.setLength(digits.length() - 1);
                return true;
            case BACK:
                if (digits.length() > 0) digits.setLength(digits.length() - 1);
                else ph.pop();
                return true;
            default: return super.onKey(k);
        }
    }

    @Override
    public boolean onClick(int x, int y) {
        if (x >= 7 && x < 259 && y >= 276 && y < 310) {
            ph.push(new SocialLinkView(ph));
            return true;
        }
        return false;
    }
}
