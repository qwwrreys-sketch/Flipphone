package com.flipphone.client.ui;

import com.flipphone.client.PhoneClientNet;
import com.flipphone.client.PhoneClientState;
import com.flipphone.net.PhoneNet;

/**
 * The payment screen: type an amount (min {@literal ¥}10, max {@literal ¥}500,000) and press the green Pay button.
 * The server checks everything again and moves the money; this screen only shows the answer.
 */
public final class PayView extends View {
    private final String tag;
    private final StringBuilder amount = new StringBuilder();
    private long sentAt;
    private boolean waiting;
    private String message = "";
    private boolean success;
    private long doneAt;

    public PayView(PhoneScreen ph, String tag) {
        super(ph);
        this.tag = tag;
    }

    @Override public String left() { return "BACK"; }
    @Override public String center() { return "PAY"; }
    @Override public String right() { return "DEL"; }
    @Override public boolean textMode() { return true; }

    private int value() {
        if (amount.length() == 0) return 0;
        try {
            return Integer.parseInt(amount.toString());
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    /** Local hint only (the server decides): "" = looks fine. */
    private String problem() {
        int v = value();
        if (v == 0) return "";
        if (v < PhoneNet.PAY_MIN) return "Minimum payment is \u00a510.";
        if (v > PhoneNet.PAY_MAX) return "Maximum payment is \u00a5500,000.";
        if (PhoneClientState.state != null && v > PhoneClientState.state.yen()) return "Not enough yen.";
        return "";
    }

    @Override
    public void render(PhoneGfx g, int mx, int my) {
        Widgets.header(g, "YENAPP");
        g.textBold("PAY", 10, 36, 14f, PhoneTheme.INK);
        g.text("To", 10, 62, 9f, PhoneTheme.MUTED);
        g.textBold(g.fit(tag, 14f, 190), 32, 59, 14f, PhoneTheme.INK);
        if (PhoneClientState.state != null) g.text("Balance: " + Fmt.yen(PhoneClientState.state.yen()), 10, 82, 9f, PhoneTheme.MUTED);

        g.rrect(6, 102, 254, 50, 9, PhoneTheme.PANEL_DARK);
        String shown = amount.length() == 0 ? "\u00a50" : Fmt.yen(value() == Integer.MAX_VALUE ? 0 : value());
        g.textR(shown, 250, 116, 22f, amount.length() == 0 ? 0xFF6F746A : PhoneTheme.WHITE);
        g.textC("MIN \u00a510   -   MAX \u00a5500,000", 133, 162, 8f, PhoneTheme.MUTED);

        // result of the payment / local hint
        PhoneClientState.YenResult r = PhoneClientState.lastYen;
        if (waiting && r != null && r.op() == PhoneNet.YEN_PAY && r.at() >= sentAt) {
            waiting = false;
            message = r.message();
            success = r.ok();
            if (success) doneAt = System.currentTimeMillis();
        }
        String hint = waiting ? "Sending..." : !message.isEmpty() ? message : problem();
        if (!hint.isEmpty()) {
            int color = success ? PhoneTheme.GREEN_BTN : (waiting ? PhoneTheme.MUTED : PhoneTheme.RED);
            g.textC(g.fit(hint, 9f, 250), 133, 188, 9f, color);
        }
        g.rrect(33, 270, 200, 36, 9, success ? 0xFF16813F : PhoneTheme.GREEN_BTN);
        g.textC(success ? "Done" : (waiting ? "..." : "Pay"), 133, 281, 14f, PhoneTheme.WHITE);
    }

    private void submit() {
        if (waiting || success) return;
        int v = value();
        if (v < PhoneNet.PAY_MIN || v > PhoneNet.PAY_MAX) {
            message = v == 0 ? "Enter an amount." : problem();
            return;
        }
        message = "";
        waiting = true;
        sentAt = System.currentTimeMillis();
        PhoneClientNet.yen(PhoneNet.YEN_PAY, tag, v);
    }

    @Override
    public void tick() {
        if (success && System.currentTimeMillis() - doneAt > 1500) ph.pop();
    }

    @Override
    public void onChar(char c) {
        if (success || waiting) return;
        if (c >= '0' && c <= '9' && amount.length() < 7) {
            if (amount.length() == 0 && c == '0') return; // no leading zeros
            amount.append(c);
            message = "";
        }
    }

    @Override
    public boolean onKey(Key k) {
        switch (k) {
            case OK: submit(); return true;
            case RIGHT_SOFT:
                if (amount.length() > 0) amount.setLength(amount.length() - 1);
                message = "";
                return true;
            case BACK:
                if (amount.length() > 0 && !success) {
                    amount.setLength(amount.length() - 1);
                    message = "";
                } else {
                    ph.pop();
                }
                return true;
            default: return super.onKey(k);
        }
    }

    @Override
    public boolean onClick(int x, int y) {
        if (x >= 33 && x < 233 && y >= 270 && y < 306) {
            submit();
            return true;
        }
        return false;
    }
}
