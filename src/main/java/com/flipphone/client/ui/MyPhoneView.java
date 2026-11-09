package com.flipphone.client.ui;

import com.flipphone.client.PhoneClientState;
import com.flipphone.net.PhoneNet;

/** "MY PHONE": own number, yen balance and activity stats (video). */
public final class MyPhoneView extends View {
    public MyPhoneView(PhoneScreen ph) {
        super(ph);
    }

    @Override public String left() { return "BACK"; }
    @Override public String center() { return ""; }
    @Override public String right() { return ""; }

    @Override
    public void render(PhoneGfx g, int mx, int my) {
        Widgets.header(g, "MY PHONE");
        PhoneNet.StateDto s = PhoneClientState.state;
        if (s == null) {
            g.textC("Loading...", 133, 140, 11f, PhoneTheme.MUTED);
            return;
        }
        // number
        g.rrect(6, 30, 254, 66, 9, PhoneTheme.PANEL_DARK);
        g.text("MY NUMBER", 16, 40, 7.5f, 0xFF8F948A);
        g.text(s.number(), 16, 62, 17f, PhoneTheme.WHITE);
        // yen
        g.rrect(6, 103, 254, 37, 8, PhoneTheme.PANEL);
        g.text("YEN", 14, 118, 7.5f, 0xFF646A5F);
        g.textR(Fmt.yen(s.yen()), 252, 114, 17f, PhoneTheme.INK_SOFT);
        // activity
        g.textC("ACTIVITY", 133, 150, 7.5f, PhoneTheme.MUTED);
        g.fill(10, 158, 246, 1, 0xFFB5BAAD);
        stat(g, 4, 167, String.valueOf(s.contacts().size()), "CONTACTS");
        stat(g, 133, 167, String.valueOf(s.msgsSent()), "MSGS SENT");
        stat(g, 4, 222, String.valueOf(s.calls()), "CALLS");
        stat(g, 133, 222, Fmt.durationCompact(s.callSeconds()), "CALL TIME");
        g.text("TOTAL MESSAGES RECEIVED", 6, 280, 7f, PhoneTheme.INK_SOFT);
        g.textR(String.valueOf(s.msgsRecv()), 260, 279, 8f, PhoneTheme.INK);
        g.rrect(6, 292, 254, 22, 6, PhoneTheme.PANEL_DARK);
        g.text("\u30c9\u30b3\u30e2", 14, 299, 8f, 0xFFB8BCB0);
        g.textR("FOMA 902i", 252, 299, 8f, 0xFFB8BCB0);
    }

    private static void stat(PhoneGfx g, int x, int y, String value, String label) {
        g.rrect(x, y, 129, 50, 8, 0xFF7B8374);
        g.rrect(x + 1, y + 1, 127, 48, 7, PhoneTheme.PANEL);
        g.textC(value, x + 64, y + 8, 16f, PhoneTheme.INK_SOFT);
        g.textC(label, x + 64, y + 34, 7f, 0xFF646A5F);
    }
}
