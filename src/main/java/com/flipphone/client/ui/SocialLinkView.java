package com.flipphone.client.ui;

import java.util.Collections;
import java.util.List;

import com.flipphone.client.PhoneClientNet;
import com.flipphone.client.PhoneClientState;
import com.flipphone.net.PhoneNet;

/**
 * SOCIALLINK: swap numbers with players standing within 5 blocks who ALSO have SocialLink open.
 * While this screen is open the phone tells the server "I am searching" once a second; the server answers with
 * the numbers of everybody nearby who is doing the same. Pick one -> "Add Contact" appears -> give it a nickname.
 */
public final class SocialLinkView extends View {
    private static final int LIST_Y = 86;
    private static final int ROW_H = 38;

    private int ticks;
    private int hi;            // highlighted row
    private String selected;   // the number that was selected (shows the Add Contact button)

    public SocialLinkView(PhoneScreen ph) {
        super(ph);
    }

    @Override public String left() { return "BACK"; }
    @Override public String center() { return selected == null ? "SELECT" : "ADD"; }
    @Override public String right() { return ""; }

    @Override
    public void onShow() {
        ticks = 0;
        PhoneClientNet.social(true);
    }

    @Override
    public void onLeave() {
        PhoneClientNet.social(false);
    }

    @Override
    public void tick() {
        if (++ticks % 20 == 0) PhoneClientNet.social(true); // heartbeat + scan, once a second
    }

    private List<String> found() {
        // results older than 3 seconds are stale (the server stopped answering = nobody nearby)
        if (System.currentTimeMillis() - PhoneClientState.socialAt > 3000) return Collections.emptyList();
        return PhoneClientState.social;
    }

    @Override
    public void render(PhoneGfx g, int mx, int my) {
        Widgets.header(g, "SOCIALLINK");
        g.textBold("Searching" + Widgets.dots(), 10, 36, 13f, PhoneTheme.INK);
        g.text("Stay within " + (int) PhoneNet.SOCIAL_RANGE + " m of a player who", 10, 56, 8f, PhoneTheme.MUTED);
        g.text("also has SocialLink open.", 10, 67, 8f, PhoneTheme.MUTED);

        List<String> list = found();
        if (selected != null && !list.contains(selected)) selected = null; // they walked away
        hi = Math.max(0, Math.min(hi, Math.max(0, list.size() - 1)));
        for (int i = 0; i < list.size() && i < 5; i++) {
            int y = LIST_Y + i * ROW_H;
            boolean isHi = i == hi;
            boolean isSel = list.get(i).equals(selected);
            g.rrect(4, y, 258, ROW_H - 4, 7, isSel ? PhoneTheme.TILE_SEL : (isHi ? PhoneTheme.TILE : PhoneTheme.PANEL));
            Widgets.avatar(g, 10, y + 3, 26);
            g.text(list.get(i), 46, y + 11, 13f, isSel ? PhoneTheme.SOFT_TEXT : PhoneTheme.INK);
            g.textR(isSel ? "SELECTED" : "NEARBY", 252, y + 12, 7.5f, isSel ? 0xFFA9AEA2 : PhoneTheme.MUTED);
        }
        if (selected != null) {
            Widgets.button(g, 7, 282, 252, 34, "Add Contact", PhoneTheme.INK, PhoneTheme.SOFT_TEXT);
            g.outline(7, 282, 252, 34, 2, PhoneTheme.LINE_GREEN);
        }
    }

    private void addContact() {
        String number = selected;
        if (number == null) return;
        ph.push(new TextInputView(ph, "ADD CONTACT", number, "", PhoneNet.NICK_MAX,
                name -> PhoneClientNet.contact(PhoneNet.CONTACT_ADD, number, name, true)));
    }

    @Override
    public boolean onKey(Key k) {
        List<String> list = found();
        switch (k) {
            case UP: if (hi > 0) hi--; return true;
            case DOWN: if (hi < list.size() - 1) hi++; return true;
            case OK:
                if (list.isEmpty()) return true;
                if (selected != null && selected.equals(list.get(Math.min(hi, list.size() - 1)))) addContact();
                else selected = list.get(Math.min(hi, list.size() - 1));
                return true;
            default: return super.onKey(k);
        }
    }

    @Override
    public boolean onClick(int x, int y) {
        if (selected != null && y >= 282 && y < 316) {
            addContact();
            return true;
        }
        List<String> list = found();
        if (y >= LIST_Y) {
            int i = (y - LIST_Y) / ROW_H;
            if (i < list.size() && i < 5) {
                hi = i;
                selected = list.get(i);
                return true;
            }
        }
        return false;
    }
}
