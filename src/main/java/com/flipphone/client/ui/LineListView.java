package com.flipphone.client.ui;

import java.util.Collections;
import java.util.List;

import com.flipphone.client.PhoneClientState;
import com.flipphone.net.PhoneNet;

/** LINE: the list of conversations, newest first (video: green "LINE" title, avatars, previews, times). */
public final class LineListView extends View {
    private static final int ROW_H = 48;
    private static final int LIST_Y = 36;
    private static final int VISIBLE = (324 - LIST_Y) / ROW_H;

    private int sel;
    private int top;

    public LineListView(PhoneScreen ph) {
        super(ph);
    }

    @Override public String left() { return "BACK"; }
    @Override public String center() { return "OPEN"; }
    @Override public String right() { return "NEW"; }

    private List<PhoneNet.ChatDto> chats() {
        return PhoneClientState.state == null ? Collections.emptyList() : PhoneClientState.state.chats();
    }

    @Override
    public void render(PhoneGfx g, int mx, int my) {
        g.fill(0, 0, 266, 324, PhoneTheme.WHITE);
        g.textBold("LINE", 9, 9, 18f, PhoneTheme.LINE_GREEN);

        List<PhoneNet.ChatDto> chats = chats();
        if (chats.isEmpty()) {
            g.textC("No messages yet.", 133, 130, 11f, PhoneTheme.GREY);
            g.textC("Press NEW to write to a contact,", 133, 152, 8f, PhoneTheme.GREY);
            g.textC("or use SOCIALLINK to add friends.", 133, 164, 8f, PhoneTheme.GREY);
            return;
        }
        sel = Math.max(0, Math.min(sel, chats.size() - 1));
        if (sel < top) top = sel;
        if (sel >= top + VISIBLE) top = sel - VISIBLE + 1;

        for (int i = 0; i < VISIBLE && top + i < chats.size(); i++) {
            PhoneNet.ChatDto c = chats.get(top + i);
            int y = LIST_Y + i * ROW_H;
            if (top + i == sel) g.fill(0, y, 266, ROW_H, PhoneTheme.ROW_SEL);
            Widgets.avatar(g, 8, y + 9, 30);
            g.text(g.fit(PhoneClientState.nameOf(c.number()), 12f, 150), 46, y + 9, 12f, PhoneTheme.NAME);
            String preview = (c.lastMine() ? "You: " : "") + c.lastText();
            g.text(g.fit(preview, 8.5f, 170), 46, y + 27, 8.5f, PhoneTheme.GREY);
            g.textR(Fmt.hm(c.lastTime()), 258, y + 10, 8f, PhoneTheme.GREY);
            g.fill(46, y + ROW_H - 1, 220, 1, PhoneTheme.ROW_LINE);
        }
    }

    private void open() {
        List<PhoneNet.ChatDto> chats = chats();
        if (!chats.isEmpty()) ph.push(new ChatView(ph, chats.get(Math.min(sel, chats.size() - 1)).number()));
    }

    @Override
    public boolean onKey(Key k) {
        switch (k) {
            case UP: if (sel > 0) sel--; return true;
            case DOWN: if (sel < chats().size() - 1) sel++; return true;
            case OK: open(); return true;
            case RIGHT_SOFT: ph.push(new ContactsView(ph, true)); return true;
            default: return super.onKey(k);
        }
    }

    @Override
    public boolean onClick(int x, int y) {
        if (y >= LIST_Y) {
            int i = top + (y - LIST_Y) / ROW_H;
            if (i < chats().size()) {
                sel = i;
                open();
                return true;
            }
        }
        return false;
    }
}
