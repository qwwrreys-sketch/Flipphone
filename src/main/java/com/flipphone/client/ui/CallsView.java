package com.flipphone.client.ui;

import java.util.Collections;
import java.util.List;

import com.flipphone.client.PhoneClientNet;
import com.flipphone.client.PhoneClientState;
import com.flipphone.net.PhoneNet;

/** "RECENT CALLS": rounded rows, the selected one dark (video). */
public final class CallsView extends View {
    private static final int ROW_H = 46;
    private static final int LIST_Y = 32;
    private static final int VISIBLE = (324 - LIST_Y) / ROW_H;

    private int sel;
    private int top;

    public CallsView(PhoneScreen ph) {
        super(ph);
    }

    private List<PhoneNet.CallDto> list() {
        return PhoneClientState.state == null ? Collections.emptyList() : PhoneClientState.state.recent();
    }

    private PhoneNet.CallDto current() {
        List<PhoneNet.CallDto> l = list();
        return l.isEmpty() ? null : l.get(Math.max(0, Math.min(sel, l.size() - 1)));
    }

    @Override public String left() { return "BACK"; }
    @Override public String center() { return "CALL"; }

    @Override
    public String right() {
        PhoneNet.CallDto c = current();
        if (c == null) return "";
        return PhoneClientState.isContact(c.number()) ? "MSG" : "SAVE";
    }

    @Override
    public void render(PhoneGfx g, int mx, int my) {
        Widgets.header(g, "RECENT CALLS");
        List<PhoneNet.CallDto> list = list();
        if (list.isEmpty()) {
            g.textC("No calls yet.", 133, 140, 11f, PhoneTheme.MUTED);
            return;
        }
        sel = Math.max(0, Math.min(sel, list.size() - 1));
        if (sel < top) top = sel;
        if (sel >= top + VISIBLE) top = sel - VISIBLE + 1;
        for (int i = 0; i < VISIBLE && top + i < list.size(); i++) {
            PhoneNet.CallDto c = list.get(top + i);
            int y = LIST_Y + i * ROW_H;
            boolean on = top + i == sel;
            g.rrect(4, y, 258, ROW_H - 4, 7, on ? PhoneTheme.TILE_SEL : PhoneTheme.TILE);
            String line1 = (c.outgoing() ? "Outgoing: " : "Incoming: ") + PhoneClientState.nameOf(c.number())
                    + (c.seconds() > 0 ? " (" + Fmt.duration(c.seconds()) + ")" : "");
            String month = Fmt.monthDay(c.time()).substring(0, 3);
            String line2 = c.outcome() + "   " + month + " @ " + Fmt.hm(c.time());
            g.text(g.fit(line1, 10f, 238), 12, y + 9, 10f, on ? PhoneTheme.SOFT_TEXT : PhoneTheme.INK_SOFT);
            g.text(g.fit(line2, 8f, 238), 12, y + 25, 8f, on ? 0xFFA9AEA2 : 0xFF5F655A);
        }
    }

    private void call() {
        PhoneNet.CallDto c = current();
        if (c == null) return;
        PhoneClientState.callPhase = PhoneClientState.CALL_NONE;
        PhoneClientNet.dial(c.number());
        ph.push(new CallView(ph));
    }

    private void messageOrSave() {
        PhoneNet.CallDto c = current();
        if (c == null) return;
        if (PhoneClientState.isContact(c.number())) {
            ph.push(new ChatView(ph, c.number()));
        } else {
            ph.push(new TextInputView(ph, "SAVE CONTACT", c.number(), "", PhoneNet.NICK_MAX,
                    name -> PhoneClientNet.contact(PhoneNet.CONTACT_ADD, c.number(), name, false)));
        }
    }

    @Override
    public boolean onKey(Key k) {
        switch (k) {
            case UP: if (sel > 0) sel--; return true;
            case DOWN: if (sel < list().size() - 1) sel++; return true;
            case OK: call(); return true;
            case RIGHT_SOFT: messageOrSave(); return true;
            default: return super.onKey(k);
        }
    }

    @Override
    public boolean onClick(int x, int y) {
        if (y >= LIST_Y) {
            int i = top + (y - LIST_Y) / ROW_H;
            if (i < list().size()) {
                sel = i;
                return true;
            }
        }
        return false;
    }
}
