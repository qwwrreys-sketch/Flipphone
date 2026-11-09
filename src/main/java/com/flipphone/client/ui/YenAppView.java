package com.flipphone.client.ui;

import com.flipphone.client.PhoneClientNet;
import com.flipphone.client.PhoneClientState;
import com.flipphone.net.PhoneNet;

/**
 * YENAPP.
 * First time: create your permanent {@literal ¥}tag (1-10 characters, can never be changed).
 * After that: your balance, a "Search a {@literal ¥}tag" bar and, when the typed tag exists, a profile card with a
 * green Pay button that opens {@link PayView}.
 */
public final class YenAppView extends View {
    private final StringBuilder text = new StringBuilder();
    private String lastSent = "";
    private long changedAt;
    private long lookupSentAt;
    private String foundTag;        // canonical tag of the player found for the text in the box (null = none yet)
    private boolean looked;         // the server answered the last lookup
    private long registerSentAt;
    private String registerMessage = "";

    public YenAppView(PhoneScreen ph) {
        super(ph);
    }

    private boolean registered() {
        return PhoneClientState.state != null && !PhoneClientState.state.tag().isEmpty();
    }

    @Override public String left() { return "BACK"; }

    @Override
    public String center() {
        if (PhoneClientState.state == null) return "";
        if (!registered()) return "CREATE";
        return foundTag != null ? "PAY" : "";
    }

    @Override public String right() { return "DEL"; }
    @Override public boolean textMode() { return true; }

    @Override
    public void render(PhoneGfx g, int mx, int my) {
        Widgets.header(g, "YENAPP");
        PhoneNet.StateDto s = PhoneClientState.state;
        if (s == null) {
            g.textC("Loading...", 133, 140, 11f, PhoneTheme.MUTED);
            return;
        }
        if (!registered()) {
            renderRegister(g);
            return;
        }
        // ---- balance
        g.rrect(6, 32, 254, 64, 9, PhoneTheme.PANEL_DARK);
        g.text("BALANCE", 16, 41, 7.5f, 0xFF8F948A);
        g.textBold(Fmt.yen(s.yen()), 16, 56, 20f, PhoneTheme.WHITE);
        g.textR("YOUR \u00a5TAG: " + s.tag(), 250, 82, 7.5f, 0xFFB8BCB0);

        // ---- search bar
        Widgets.inputBox(g, 6, 106, 254, 34, text.toString(), "Search a \u00a5tag", 12f, foundTag == null);
        if (foundTag == null) {
            if (looked && text.length() > 0) g.textC("No player has that \u00a5tag.", 133, 152, 8.5f, PhoneTheme.RED);
            else g.textC("Type an exact \u00a5tag to find a player.", 133, 152, 8f, PhoneTheme.MUTED);
            return;
        }
        // ---- profile card with the green Pay button
        g.rrect(13, 156, 240, 148, 12, PhoneTheme.WHITE);
        g.circle(133, 193, 21, PhoneTheme.AVATAR);
        g.circle(133, 187, 7, PhoneTheme.AVATAR_ICON);
        g.rrect(121, 196, 24, 14, 7, PhoneTheme.AVATAR_ICON);
        g.textC(foundTag, 133, 222, 14f, PhoneTheme.NAME);
        g.textC("YenApp wallet", 133, 240, 8f, PhoneTheme.GREY);
        g.rrect(43, 262, 180, 34, 8, PhoneTheme.GREEN_BTN);
        g.textC("Pay", 133, 272, 13f, PhoneTheme.WHITE);
    }

    private void renderRegister(PhoneGfx g) {
        g.textBold("CREATE YOUR \u00a5TAG", 10, 38, 12f, PhoneTheme.INK);
        g.text("Your \u00a5tag is your name in YenApp.", 10, 58, 8f, PhoneTheme.MUTED);
        g.text("1-10 letters, numbers, _ or .", 10, 70, 8f, PhoneTheme.MUTED);
        g.textBold("It is PERMANENT and can never", 10, 86, 8f, PhoneTheme.RED);
        g.textBold("be changed.", 10, 97, 8f, PhoneTheme.RED);
        Widgets.inputBox(g, 6, 120, 254, 40, text.toString(), "\u00a5tag", 15f, true);
        g.textR(text.length() + "/" + PhoneNet.TAG_MAX, 256, 166, 7.5f, PhoneTheme.MUTED);

        PhoneClientState.YenResult r = PhoneClientState.lastYen;
        if (r != null && r.op() == PhoneNet.YEN_REGISTER && r.at() >= registerSentAt && !r.ok()) registerMessage = r.message();
        if (!registerMessage.isEmpty()) g.textC(g.fit(registerMessage, 8.5f, 250), 133, 186, 8.5f, PhoneTheme.RED);
        Widgets.button(g, 33, 270, 200, 34, "Create \u00a5tag", PhoneTheme.GREEN_BTN, PhoneTheme.WHITE);
    }

    private void register() {
        if (text.length() < PhoneNet.TAG_MIN) return;
        registerSentAt = System.currentTimeMillis();
        registerMessage = "";
        PhoneClientNet.yen(PhoneNet.YEN_REGISTER, text.toString(), 0);
    }

    private void edited() {
        changedAt = System.currentTimeMillis();
        foundTag = null;
        looked = false;
    }

    @Override
    public void tick() {
        if (!registered()) return;
        String now = text.toString();
        if (now.isEmpty()) {
            lastSent = "";
            foundTag = null;
            looked = false;
            return;
        }
        // wait 250 ms after the last keystroke, then ask the server whether this exact tag exists
        if (!now.equals(lastSent) && System.currentTimeMillis() - changedAt >= 250) {
            lastSent = now;
            lookupSentAt = System.currentTimeMillis();
            PhoneClientNet.yen(PhoneNet.YEN_LOOKUP, now, 0);
        }
        PhoneClientState.YenResult r = PhoneClientState.lastYen;
        if (r != null && r.op() == PhoneNet.YEN_LOOKUP && r.at() >= lookupSentAt && now.equals(lastSent)) {
            looked = true;
            foundTag = r.ok() && r.tag().equalsIgnoreCase(now) ? r.tag() : null;
        }
    }

    @Override
    public void onChar(char c) {
        boolean ok = Character.isLetterOrDigit(c) && c < 128 || c == '_' || c == '.';
        if (ok && text.length() < PhoneNet.TAG_MAX) {
            text.append(c);
            edited();
        }
    }

    @Override
    public boolean onKey(Key k) {
        switch (k) {
            case OK:
                if (!registered()) register();
                else if (foundTag != null) ph.push(new PayView(ph, foundTag));
                return true;
            case RIGHT_SOFT:
                if (text.length() > 0) {
                    text.setLength(text.length() - 1);
                    edited();
                }
                return true;
            case BACK:
                if (text.length() > 0) {
                    text.setLength(text.length() - 1);
                    edited();
                } else {
                    ph.pop();
                }
                return true;
            default: return super.onKey(k);
        }
    }

    @Override
    public boolean onClick(int x, int y) {
        if (!registered()) {
            if (x >= 33 && x < 233 && y >= 270 && y < 304) {
                register();
                return true;
            }
        } else if (foundTag != null && x >= 43 && x < 223 && y >= 262 && y < 296) {
            ph.push(new PayView(ph, foundTag));
            return true;
        }
        return false;
    }
}
