package com.flipphone.client.ui;

import com.flipphone.client.PhoneClientNet;
import com.flipphone.client.PhoneClientState;
import com.flipphone.net.PhoneNet;

/**
 * The call screen: calling / incoming / in call (with a running timer) / call ended.
 * Calls are for roleplay immersion only: no audio and no cost; the stats on "MY PHONE" count them.
 */
public final class CallView extends View {
    private final long created = System.currentTimeMillis();

    public CallView(PhoneScreen ph) {
        super(ph);
    }

    private boolean ended() {
        return PhoneClientState.callPhase == PhoneNet.EVT_ENDED && PhoneClientState.callEndedAt >= created - 50;
    }

    private int phase() {
        if (ended()) return PhoneNet.EVT_ENDED;
        int p = PhoneClientState.callPhase;
        return p == PhoneNet.EVT_RINGING_OUT || p == PhoneNet.EVT_INCOMING || p == PhoneNet.EVT_ACTIVE ? p : -2; // -2 = connecting
    }

    @Override
    public String left() {
        int p = phase();
        if (p == PhoneNet.EVT_ENDED) return "BACK";
        return p == PhoneNet.EVT_INCOMING ? "DENY" : "HANG UP";
    }

    @Override
    public String center() {
        return phase() == PhoneNet.EVT_INCOMING ? "ANSWER" : "";
    }

    @Override public String right() { return ""; }

    @Override
    public void render(PhoneGfx g, int mx, int my) {
        g.fill(0, 0, 266, 324, PhoneTheme.TILE_SEL);
        int p = phase();
        String status;
        switch (p) {
            case PhoneNet.EVT_INCOMING: status = "INCOMING CALL"; break;
            case PhoneNet.EVT_ACTIVE: status = "IN CALL"; break;
            case PhoneNet.EVT_ENDED: status = PhoneClientState.callEndReason; break;
            case PhoneNet.EVT_RINGING_OUT: status = "CALLING" + Widgets.dots(); break;
            default: status = "CONNECTING" + Widgets.dots(); break;
        }
        g.textC(status, 133, 40, 11f, 0xFFA9AEA2);

        String number = PhoneClientState.callNumber;
        g.circle(133, 112, 34, PhoneTheme.AVATAR);
        g.circle(133, 102, 11, PhoneTheme.AVATAR_ICON);
        g.rrect(114, 118, 38, 22, 11, PhoneTheme.AVATAR_ICON);
        g.textC(g.fit(PhoneClientState.nameOf(number), 15f, 240), 133, 166, 15f, PhoneTheme.SOFT_TEXT);
        if (!PhoneClientState.nameOf(number).equals(number)) g.textC(number, 133, 188, 10f, 0xFFA9AEA2);

        if (p == PhoneNet.EVT_ACTIVE) {
            long secs = (System.currentTimeMillis() - PhoneClientState.callActiveSince) / 1000;
            g.textC(Fmt.clock(secs), 133, 226, 20f, PhoneTheme.SOFT_TEXT);
        } else if (p == PhoneNet.EVT_ENDED && PhoneClientState.callEndSeconds > 0) {
            g.textC(Fmt.duration(PhoneClientState.callEndSeconds), 133, 226, 14f, PhoneTheme.SOFT_TEXT);
        }
    }

    @Override
    public void tick() {
        if (ended() && System.currentTimeMillis() - PhoneClientState.callEndedAt > 1800) ph.pop();
    }

    @Override
    public boolean onKey(Key k) {
        int p = phase();
        switch (k) {
            case OK:
                if (p == PhoneNet.EVT_INCOMING) PhoneClientNet.callAction(PhoneNet.CALL_ANSWER);
                return true;
            case LEFT_SOFT:
            case BACK:
                if (p == PhoneNet.EVT_ENDED) ph.pop();
                else PhoneClientNet.callAction(PhoneNet.CALL_HANGUP);
                return true;
            case MENU:
                return true;
            default:
                return true;
        }
    }
}
