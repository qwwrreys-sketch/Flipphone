package com.flipphone.client.ui;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.flipphone.client.PhoneClientNet;
import com.flipphone.client.PhoneClientState;
import com.flipphone.net.PhoneNet;

/**
 * The saved contacts (video: LINE "Contacts" list with name + number and a popup menu).
 * In "pick" mode it is used by LINE's NEW button to choose who to write to.
 */
public final class ContactsView extends View {
    private static final int ROW_H = 48;
    private static final int LIST_Y = 36;
    private static final int VISIBLE = (324 - LIST_Y) / ROW_H;

    private final boolean pick;
    private int sel;
    private int top;

    public ContactsView(PhoneScreen ph, boolean pick) {
        super(ph);
        this.pick = pick;
    }

    @Override public String left() { return "BACK"; }
    @Override public String center() { return pick ? "SELECT" : "CALL"; }
    @Override public String right() { return pick ? "" : "MORE"; }

    private List<PhoneNet.ContactDto> list() {
        return PhoneClientState.state == null ? Collections.emptyList() : PhoneClientState.state.contacts();
    }

    @Override
    public void render(PhoneGfx g, int mx, int my) {
        g.fill(0, 0, 266, 324, PhoneTheme.WHITE);
        g.textBold("LINE", 9, 9, 18f, PhoneTheme.LINE_GREEN);
        g.textR("Contacts", 258, 14, 9f, PhoneTheme.GREY);

        List<PhoneNet.ContactDto> list = list();
        if (list.isEmpty()) {
            g.textC("No contacts yet.", 133, 130, 11f, PhoneTheme.GREY);
            g.textC("Use SOCIALLINK next to a friend", 133, 152, 8f, PhoneTheme.GREY);
            g.textC("to exchange numbers.", 133, 164, 8f, PhoneTheme.GREY);
            return;
        }
        sel = Math.max(0, Math.min(sel, list.size() - 1));
        if (sel < top) top = sel;
        if (sel >= top + VISIBLE) top = sel - VISIBLE + 1;
        for (int i = 0; i < VISIBLE && top + i < list.size(); i++) {
            PhoneNet.ContactDto c = list.get(top + i);
            int y = LIST_Y + i * ROW_H;
            if (top + i == sel) g.fill(0, y, 266, ROW_H, PhoneTheme.ROW_SEL);
            Widgets.avatar(g, 8, y + 9, 30);
            g.text(g.fit(c.name(), 12f, 200), 46, y + 9, 12f, PhoneTheme.NAME);
            g.text(c.number(), 46, y + 27, 8.5f, PhoneTheme.GREY);
            g.fill(46, y + ROW_H - 1, 220, 1, PhoneTheme.ROW_LINE);
        }
    }

    private PhoneNet.ContactDto current() {
        List<PhoneNet.ContactDto> list = list();
        return list.isEmpty() ? null : list.get(Math.max(0, Math.min(sel, list.size() - 1)));
    }

    private void primary() {
        PhoneNet.ContactDto c = current();
        if (c == null) return;
        if (pick) {
            ph.pop();
            ph.push(new ChatView(ph, c.number()));
        } else {
            call(c.number());
        }
    }

    private void call(String number) {
        PhoneClientState.callPhase = PhoneClientState.CALL_NONE;
        PhoneClientNet.dial(number);
        ph.push(new CallView(ph));
    }

    private void more() {
        PhoneNet.ContactDto c = current();
        if (c == null) return;
        ph.push(new PopupMenuView(ph, Arrays.asList("Call", "Message", "Rename", "Delete"), i -> {
            switch (i) {
                case 0: call(c.number()); break;
                case 1: ph.push(new ChatView(ph, c.number())); break;
                case 2:
                    ph.push(new TextInputView(ph, "RENAME", c.number(), c.name(), PhoneNet.NICK_MAX,
                            name -> PhoneClientNet.contact(PhoneNet.CONTACT_RENAME, c.number(), name, false)));
                    break;
                default: PhoneClientNet.contact(PhoneNet.CONTACT_DELETE, c.number(), "", false); break;
            }
        }));
    }

    @Override
    public boolean onKey(Key k) {
        switch (k) {
            case UP: if (sel > 0) sel--; return true;
            case DOWN: if (sel < list().size() - 1) sel++; return true;
            case OK: primary(); return true;
            case RIGHT_SOFT: if (!pick) more(); return true;
            default: return super.onKey(k);
        }
    }

    @Override
    public boolean onClick(int x, int y) {
        if (y >= LIST_Y) {
            int i = top + (y - LIST_Y) / ROW_H;
            if (i < list().size()) {
                sel = i;
                if (pick) primary();
                else more();
                return true;
            }
        }
        return false;
    }
}
