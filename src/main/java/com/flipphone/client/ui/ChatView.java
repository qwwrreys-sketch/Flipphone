package com.flipphone.client.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.flipphone.client.PhoneClientNet;
import com.flipphone.client.PhoneClientState;
import com.flipphone.net.PhoneNet;

import net.minecraft.util.FormattedCharSequence;

/**
 * One conversation (video: blue background, green bubbles on the right, white on the left, date pills,
 * an input pill with a green send button). Messages arrive and are sent in real time.
 */
public final class ChatView extends View {
    private static final int HEADER_H = 30;
    private static final int INPUT_Y = 284;
    private static final float TEXT = 10.5f;
    private static final int MAX_TEXT_W = 140;

    private final String number;
    private final StringBuilder input = new StringBuilder();
    private int scroll;           // how far the user scrolled up (0 = at the newest message)
    private int builtVersion = -1;
    private int builtSize = -1;

    private static final class Item {
        boolean pill;
        String pillText = "";
        boolean mine;
        List<FormattedCharSequence> lines = Collections.emptyList();
        float textW;
        String time = "";
        int h;
    }

    private final List<Item> items = new ArrayList<>();
    private int totalH;

    public ChatView(PhoneScreen ph, String number) {
        super(ph);
        this.number = number;
    }

    @Override public String left() { return "BACK"; }
    @Override public String center() { return "SEND"; }
    @Override public String right() { return "CALL"; }
    @Override public boolean textMode() { return true; }

    @Override
    public void onShow() {
        PhoneClientNet.openChat(number);
    }

    private void rebuild(PhoneGfx g) {
        items.clear();
        totalH = 0;
        List<PhoneNet.MsgDto> list = PhoneClientState.history.getOrDefault(number, Collections.emptyList());
        String lastDay = "";
        for (PhoneNet.MsgDto m : list) {
            String day = Fmt.monthDay(m.time());
            if (!day.equals(lastDay)) {
                Item p = new Item();
                p.pill = true;
                p.pillText = day;
                p.h = 24;
                items.add(p);
                totalH += p.h;
                lastDay = day;
            }
            Item it = new Item();
            it.mine = m.mine();
            it.lines = g.wrap(m.text(), TEXT, MAX_TEXT_W);
            float w = 0;
            for (FormattedCharSequence s : it.lines) w = Math.max(w, g.width(seqToString(s), TEXT));
            it.textW = w;
            it.time = Fmt.hm(m.time());
            it.h = it.lines.size() * 13 + 12;
            items.add(it);
            totalH += it.h + 6;
        }
        builtVersion = PhoneClientState.version;
        builtSize = list.size();
    }

    /** Width of a wrapped line: rebuild the plain text from the formatted sequence. */
    private static String seqToString(FormattedCharSequence seq) {
        StringBuilder sb = new StringBuilder();
        seq.accept((index, style, cp) -> {
            sb.appendCodePoint(cp);
            return true;
        });
        return sb.toString();
    }

    @Override
    public void render(PhoneGfx g, int mx, int my) {
        if (builtVersion != PhoneClientState.version) rebuild(g);

        // header: name + a "block" style icon on the right
        g.fill(0, 0, 266, HEADER_H, PhoneTheme.WHITE);
        g.textBold(g.fit(PhoneClientState.nameOf(number), 12f, 200), 8, 9, 12f, PhoneTheme.NAME);
        g.outline(238, 8, 14, 14, 2, PhoneTheme.GREY);

        // chat area
        int areaH = INPUT_Y - HEADER_H;
        g.fill(0, HEADER_H, 266, areaH, PhoneTheme.CHAT_BG);
        g.clip(0, HEADER_H, 266, areaH);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, totalH - areaH + 12)));
        int y = INPUT_Y - 6 + scroll;
        for (int i = items.size() - 1; i >= 0; i--) {
            Item it = items.get(i);
            y -= it.h + (it.pill ? 0 : 6);
            if (y > INPUT_Y) continue;
            if (y + it.h < HEADER_H) break;
            if (it.pill) {
                int w = (int) g.width(it.pillText, 8f) + 18;
                g.rrect(133 - w / 2, y + 3, w, 17, 8, PhoneTheme.PILL);
                g.textC(it.pillText, 133, y + 8, 8f, PhoneTheme.WHITE);
                continue;
            }
            int bw = (int) it.textW + 18;
            int bx = it.mine ? 266 - 8 - bw : 8;
            g.rrect(bx, y, bw, it.h, 9, it.mine ? PhoneTheme.BUBBLE_MINE : PhoneTheme.WHITE);
            for (int l = 0; l < it.lines.size(); l++) {
                g.drawSeq(it.lines.get(l), bx + 9, y + 6 + l * 13, TEXT, it.mine ? PhoneTheme.WHITE : PhoneTheme.NAME);
            }
            if (it.mine) g.textR(it.time, bx - 4, y + it.h - 11, 7.5f, 0xFFE8EEF6);
            else g.text(it.time, bx + bw + 4, y + it.h - 11, 7.5f, 0xFFE8EEF6);
        }
        if (items.isEmpty()) g.textC("Say hello!", 133, 150, 11f, 0xFFE8EEF6);
        g.unclip();

        // input bar
        g.fill(0, INPUT_Y, 266, 40, PhoneTheme.WHITE);
        g.rrect(8, INPUT_Y + 6, 208, 28, 14, PhoneTheme.INPUT_BG);
        String shown = g.fit(input.toString(), 10.5f, 180);
        if (input.length() == 0) {
            g.text("Aa", 20, INPUT_Y + 14, 10.5f, 0xFF9AA2AA);
        } else {
            g.text(shown, 20, INPUT_Y + 14, 10.5f, PhoneTheme.NAME);
        }
        if (Widgets.blink()) g.fill((int) (20 + (input.length() == 0 ? 0 : g.width(shown, 10.5f))), INPUT_Y + 11, 1, 14, PhoneTheme.NAME);
        g.circle(237, INPUT_Y + 20, 14, PhoneTheme.LINE_GREEN);
        g.textC("\u25b6", 238, INPUT_Y + 15, 11f, PhoneTheme.WHITE);
    }

    private void send() {
        String t = input.toString().trim();
        if (t.isEmpty()) return;
        PhoneClientNet.sendMessage(number, t);
        input.setLength(0);
        scroll = 0;
    }

    private void call() {
        PhoneClientState.callPhase = PhoneClientState.CALL_NONE;
        PhoneClientNet.dial(number);
        ph.push(new CallView(ph));
    }

    @Override
    public boolean onKey(Key k) {
        switch (k) {
            case OK: send(); return true;
            case RIGHT_SOFT: call(); return true;
            case UP: scroll += 24; return true;
            case DOWN: scroll = Math.max(0, scroll - 24); return true;
            case BACK:
                if (input.length() > 0) input.setLength(input.length() - 1);
                else ph.pop();
                return true;
            default: return super.onKey(k);
        }
    }

    @Override
    public void onChar(char c) {
        if (input.length() < PhoneNet.MSG_MAX && c != '\u00a7' && !Character.isISOControl(c)) input.append(c);
    }

    @Override
    public boolean onClick(int x, int y) {
        if (y >= INPUT_Y && x >= 222) {
            send();
            return true;
        }
        return false;
    }
}
