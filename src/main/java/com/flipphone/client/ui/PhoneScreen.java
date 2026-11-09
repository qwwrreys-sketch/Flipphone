package com.flipphone.client.ui;

import java.util.ArrayList;
import java.util.List;

import com.flipphone.client.FlipPhoneClient;
import com.flipphone.client.PhoneClientNet;
import com.flipphone.client.PhoneClientState;
import com.flipphone.net.PhoneNet;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * The flip phone: a screen that shows the phone on the right side of the window (upper half with the LCD, lower
 * half with the d-pad and number keys) exactly like the reference video, slides it in from the bottom, and runs
 * a stack of {@link View}s on the LCD. The world behind it is NOT dimmed.
 *
 * <p>Controls: Alt (or the key you chose) = toggle, arrows = navigate, Enter = OK, Backspace = Back / delete,
 * M = menu, 0-9 = dial, Tab = right soft key; everything can also be clicked with the mouse.
 */
public final class PhoneScreen extends Screen {
    // ---- phone geometry in units (taken from the video: the whole phone is 312 x 828)
    public static final int W = 312;
    public static final int H = 828;
    public static final int LCD_X = 23;
    public static final int LCD_Y = 50;
    public static final int LCD_W = 266;
    public static final int STATUS_H = 28;
    public static final int CONTENT_Y = LCD_Y + STATUS_H;   // 78
    public static final int CONTENT_H = 324;
    public static final int SOFT_Y = CONTENT_Y + CONTENT_H; // 402
    public static final int SOFT_H = 29;

    private static final int UPPER_H = 450;
    private static final int LOWER_Y = 463;

    // d-pad and keys
    private static final int DPAD_X = 156;
    private static final int DPAD_Y = 518;
    private static final int DPAD_R = 46;
    private static final int DPAD_OK_R = 18;
    private static final int SOFT_L_X = 14;
    private static final int SOFT_R_X = 232;
    private static final int SOFT_PAD_Y = 500;
    private static final int[] KEY_COL = {12, 110, 208};
    private static final int[] KEY_ROW = {572, 618, 664, 710};
    private static final int KEY_W = 92;
    private static final int KEY_H = 38;
    private static final String[] KEY_MAIN = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#"};
    private static final String[] KEY_KANA = {"\u3042", "\u304b", "\u3055", "\u305f", "\u306a", "\u306f", "\u307e", "\u3084", "\u3089", "*", "\u308f", "#"};
    private static final String[] KEY_ABC = {"", "ABC", "DEF", "GHI", "JKL", "MNO", "PQRS", "TUV", "WXYZ", "", "+", ""};

    private final List<View> stack = new ArrayList<>();
    private float scale;
    private float ox;
    private float oy;
    private final long openedAt = System.currentTimeMillis();
    private boolean closing;
    private long closeAt;
    private String pressed = "";
    private long pressedAt;

    public PhoneScreen() {
        super(Component.literal("Phone"));
    }

    // ================================================================== stack

    public View top() {
        return stack.get(stack.size() - 1);
    }

    public void push(View v) {
        stack.add(v);
        v.onShow();
    }

    /** Goes back one screen; on the home screen it does nothing. */
    public void pop() {
        if (stack.size() <= 1) return;
        View gone = stack.remove(stack.size() - 1);
        gone.onLeave();
        top().onShow();
    }

    /** Back to the home screen. */
    public void home() {
        while (stack.size() > 1) {
            View gone = stack.remove(stack.size() - 1);
            gone.onLeave();
        }
        top().onShow();
    }

    /** Jumps to the app grid (the M key / the MENU button). */
    public void openMenu() {
        if (top() instanceof MenuView) return;
        home();
        push(new MenuView(this));
    }

    // ================================================================== lifecycle

    @Override
    protected void init() {
        if (stack.isEmpty()) {
            stack.add(new HomeView(this));
            if (PhoneClientState.inCall()) push(new CallView(this));
        }
        PhoneClientNet.requestState();
        layout();
    }

    private void layout() {
        scale = Math.min(height * 0.92f / H, width * 0.46f / W);
        float margin = Math.max(6f, width * 0.02f);
        ox = width - W * scale - margin;
        oy = (height - H * scale) / 2f;
    }

    @Override
    public void resize(Minecraft mc, int w, int h) {
        super.resize(mc, w, h);
        layout();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (closing) return;
        closing = true;
        closeAt = System.currentTimeMillis();
    }

    @Override
    public void removed() {
        for (View v : stack) v.onLeave();
        super.removed();
    }

    @Override
    public void tick() {
        top().tick();
        // somebody is calling us while the phone is open: show the call screen
        if (PhoneClientState.callPhase == PhoneNet.EVT_INCOMING && !(top() instanceof CallView)) push(new CallView(this));
    }

    // ================================================================== input

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (closing) return true;
        if (FlipPhoneClient.TOGGLE != null && FlipPhoneClient.TOGGLE.matches(key, scan)) {
            onClose();
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        Key k = null;
        switch (key) {
            case GLFW.GLFW_KEY_UP: k = Key.UP; break;
            case GLFW.GLFW_KEY_DOWN: k = Key.DOWN; break;
            case GLFW.GLFW_KEY_LEFT: k = Key.LEFT; break;
            case GLFW.GLFW_KEY_RIGHT: k = Key.RIGHT; break;
            case GLFW.GLFW_KEY_ENTER:
            case GLFW.GLFW_KEY_KP_ENTER: k = Key.OK; break;
            case GLFW.GLFW_KEY_BACKSPACE: k = Key.BACK; break;
            case GLFW.GLFW_KEY_TAB: k = Key.RIGHT_SOFT; break;
            case GLFW.GLFW_KEY_DELETE: k = Key.RIGHT_SOFT; break;
            case GLFW.GLFW_KEY_M: if (!top().textMode()) k = Key.MENU; break;
            default: break;
        }
        if (k != null) {
            press(k, idOf(k));
            return true;
        }
        return true; // swallow everything else: the phone owns the keyboard while it is open
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (closing || c < 32 || c == 127) return true;
        top().onChar(c);
        if (c >= '0' && c <= '9' || c == '*' || c == '#') flash(c == '*' ? "k9" : c == '#' ? "k11" : c == '0' ? "k10" : "k" + (c - '1'));
        return true;
    }

    private static String idOf(Key k) {
        switch (k) {
            case UP: return "up";
            case DOWN: return "down";
            case LEFT: return "left";
            case RIGHT: return "right";
            case OK: return "ok";
            case LEFT_SOFT: return "softL";
            case RIGHT_SOFT: return "softR";
            default: return "";
        }
    }

    private void flash(String id) {
        pressed = id;
        pressedAt = System.currentTimeMillis();
    }

    private void press(Key k, String id) {
        if (!id.isEmpty()) flash(id);
        top().onKey(k);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (closing) return true;
        press(delta > 0 ? Key.UP : Key.DOWN, "");
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (closing || button != 0) return true;
        float ux = (float) ((mouseX - ox) / scale);
        float uy = (float) ((mouseY - currentOffsetY() - oy) / scale);

        // ---- the LCD
        if (ux >= LCD_X && ux < LCD_X + LCD_W) {
            if (uy >= CONTENT_Y && uy < CONTENT_Y + CONTENT_H) {
                top().onClick((int) (ux - LCD_X), (int) (uy - CONTENT_Y));
                return true;
            }
            if (uy >= SOFT_Y && uy < SOFT_Y + SOFT_H) {
                float lx = ux - LCD_X;
                if (lx < LCD_W / 3f) press(Key.LEFT_SOFT, "softL");
                else if (lx < LCD_W * 2f / 3f) press(Key.OK, "ok");
                else press(Key.RIGHT_SOFT, "softR");
                return true;
            }
        }
        // ---- soft key buttons beside the d-pad
        if (uy >= SOFT_PAD_Y && uy < SOFT_PAD_Y + 36) {
            if (ux >= SOFT_L_X && ux < SOFT_L_X + 66) { press(Key.LEFT_SOFT, "softL"); return true; }
            if (ux >= SOFT_R_X && ux < SOFT_R_X + 66) { press(Key.RIGHT_SOFT, "softR"); return true; }
        }
        // ---- d-pad
        float dx = ux - DPAD_X;
        float dy = uy - DPAD_Y;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist <= DPAD_R) {
            if (dist <= DPAD_OK_R) press(Key.OK, "ok");
            else if (Math.abs(dx) > Math.abs(dy)) press(dx < 0 ? Key.LEFT : Key.RIGHT, dx < 0 ? "left" : "right");
            else press(dy < 0 ? Key.UP : Key.DOWN, dy < 0 ? "up" : "down");
            return true;
        }
        // ---- number keys
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 3; c++) {
                if (ux >= KEY_COL[c] && ux < KEY_COL[c] + KEY_W && uy >= KEY_ROW[r] && uy < KEY_ROW[r] + KEY_H) {
                    int i = r * 3 + c;
                    flash("k" + i);
                    top().onChar(KEY_MAIN[i].charAt(0));
                    return true;
                }
            }
        }
        return true;
    }

    // ================================================================== animation

    /** Extra downward offset (in real pixels) while sliding in or out. */
    private float currentOffsetY() {
        long now = System.currentTimeMillis();
        float away = height - oy + 30f;
        if (closing) {
            float t = Math.min(1f, (now - closeAt) / 200f);
            return t * t * t * away;               // ease-in: slides down and away (0.2 s, like the video)
        }
        float t = Math.min(1f, (now - openedAt) / 250f);
        float inv = 1f - t;
        return (inv * inv * inv) * away;           // ease-out: slides up (0.25 s, like the video)
    }

    // ================================================================== rendering

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        // note: no renderBackground() -> the world stays fully visible, like in the video
        if (closing && System.currentTimeMillis() - closeAt >= 200) {
            Minecraft.getInstance().setScreen(null);
            return;
        }
        float offY = currentOffsetY();
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(ox, oy + offY, 0f);
        ps.scale(scale, scale, 1f);
        PhoneGfx gx = new PhoneGfx(g, this.font, scale, ox, oy + offY);

        float umx = (mx - ox) / scale;
        float umy = (my - oy - offY) / scale;

        drawBody(gx);
        drawLcd(gx, (int) umx - LCD_X, (int) umy - CONTENT_Y);
        drawPad(gx, umx, umy);

        ps.popPose();
    }

    private void drawBody(PhoneGfx g) {
        // upper half: brushed metal shell with a dark edge
        g.rrect(0, 0, W, UPPER_H, 40, PhoneTheme.BODY_EDGE);
        g.rrectGrad(2, 2, W - 4, UPPER_H - 4, 38, PhoneTheme.METAL_TOP, PhoneTheme.METAL_BOTTOM);
        g.rrect(14, 38, W - 28, 404, 16, PhoneTheme.BEZEL);                   // black bezel around the LCD
        // earpiece, camera, sensor
        g.rrect(103, 22, 106, 16, 8, 0xFF1A1A1D);
        g.rrect(106, 25, 100, 10, 5, 0xFF08080A);
        g.circle(77, 33, 7, 0xFF1A1A1D);
        g.circle(77, 33, 4, 0xFF10203A);
        g.circle(236, 33, 3, 0xFF0D0D0F);
        // hinge
        g.fill(0, UPPER_H, W, LOWER_Y - UPPER_H, 0xFF0A0A0C);
        g.fill(10, UPPER_H + 2, W - 20, 2, 0xFF2C2C30);
        // lower half
        g.rrect(0, LOWER_Y, W, H - LOWER_Y, 40, PhoneTheme.BODY_EDGE);
        g.rrectGrad(2, LOWER_Y + 2, W - 4, H - LOWER_Y - 4, 38, PhoneTheme.PAD_TOP, PhoneTheme.PAD_BOTTOM);
        g.rrect(10, LOWER_Y + 11, W - 20, H - LOWER_Y - 22, 30, PhoneTheme.PAD_PANEL);
        g.rrect(130, 806, 52, 6, 3, 0xFF17171A);
    }

    private void drawLcd(PhoneGfx g, int cmx, int cmy) {
        g.push(LCD_X, LCD_Y);
        g.rrect(0, 0, LCD_W, 381, 6, PhoneTheme.LCD);

        // ---- status bar: signal, carrier, battery
        g.fill(0, 0, LCD_W, STATUS_H, PhoneTheme.STATUS_BAR);
        for (int i = 0; i < 4; i++) g.fill(8 + i * 4, 18 - i * 3, 3, 3 + i * 3, 0xFFD8DBD0);
        g.textC("\u30c9\u30b3\u30e2", LCD_W / 2f, 9, 8.5f, 0xFFBFC3B8);
        g.outline(LCD_W - 30, 9, 19, 10, 1, 0xFFD8DBD0);
        g.fill(LCD_W - 11, 12, 2, 4, 0xFFD8DBD0);
        g.fill(LCD_W - 28, 11, 15, 6, 0xFFD8DBD0);

        // ---- content (the active view; popups are drawn over the view below them)
        g.push(0, STATUS_H);
        g.clip(0, 0, LCD_W, CONTENT_H);
        g.fill(0, 0, LCD_W, CONTENT_H, PhoneTheme.LCD);
        int base = stack.size() - 1;
        while (base > 0 && stack.get(base).isOverlay()) base--;
        for (int i = base; i < stack.size(); i++) stack.get(i).render(g, cmx, cmy);
        drawToast(g);
        g.unclip();
        g.pop();

        // ---- soft key bar
        View v = top();
        g.fill(0, STATUS_H + CONTENT_H, LCD_W, SOFT_H, PhoneTheme.SOFT_BAR);
        float ty = STATUS_H + CONTENT_H + 10;
        g.text(v.left(), 9, ty, 10f, PhoneTheme.SOFT_TEXT);
        g.textC(v.center(), LCD_W / 2f, ty, 10f, PhoneTheme.SOFT_TEXT);
        g.textR(v.right(), LCD_W - 9, ty, 10f, PhoneTheme.SOFT_TEXT);
        g.pop();
    }

    /** Short messages from the server ("Contact saved.", "You received 500 yen ...") in a dark bar on the LCD. */
    private void drawToast(PhoneGfx g) {
        if (System.currentTimeMillis() >= PhoneClientState.noticeUntil || PhoneClientState.notice.isEmpty()) return;
        String t = g.fit(PhoneClientState.notice, 9f, LCD_W - 24);
        g.rrect(8, 4, LCD_W - 16, 20, 6, 0xEE191B18);
        g.textC(t, LCD_W / 2f, 9, 9f, PhoneTheme.SOFT_TEXT);
    }

    private boolean isPressed(String id) {
        return id.equals(pressed) && System.currentTimeMillis() - pressedAt < 140;
    }

    private void drawPad(PhoneGfx g, float mx, float my) {
        // ---- soft key buttons
        for (int s = 0; s < 2; s++) {
            int x = s == 0 ? SOFT_L_X : SOFT_R_X;
            String id = s == 0 ? "softL" : "softR";
            boolean hover = mx >= x && mx < x + 66 && my >= SOFT_PAD_Y && my < SOFT_PAD_Y + 36;
            g.rrect(x - 1, SOFT_PAD_Y - 1, 68, 38, 15, PhoneTheme.KEY_EDGE);
            g.rrect(x, SOFT_PAD_Y, 66, 36, 14, isPressed(id) ? PhoneTheme.KEY_PRESS : hover ? PhoneTheme.KEY_HOVER : PhoneTheme.KEY);
        }
        // ---- d-pad
        g.circle(DPAD_X, DPAD_Y, DPAD_R + 2, PhoneTheme.KEY_EDGE);
        g.circle(DPAD_X, DPAD_Y, DPAD_R, 0xFF121215);
        float ddx = mx - DPAD_X;
        float ddy = my - DPAD_Y;
        boolean overPad = Math.sqrt(ddx * ddx + ddy * ddy) <= DPAD_R;
        if (overPad && Math.sqrt(ddx * ddx + ddy * ddy) > DPAD_OK_R) {
            // light up the quarter the mouse is on
            String zone = Math.abs(ddx) > Math.abs(ddy) ? (ddx < 0 ? "left" : "right") : (ddy < 0 ? "up" : "down");
            if (!isPressed(zone)) drawZone(g, zone, PhoneTheme.KEY_HOVER);
        }
        for (String z : new String[] {"up", "down", "left", "right"}) if (isPressed(z)) drawZone(g, z, PhoneTheme.KEY_PRESS);
        g.circle(DPAD_X, DPAD_Y, DPAD_OK_R + 1, PhoneTheme.KEY_EDGE);
        g.circle(DPAD_X, DPAD_Y, DPAD_OK_R, isPressed("ok") ? PhoneTheme.KEY_PRESS : 0xFF1D1D21);
        g.textC("OK", DPAD_X, DPAD_Y - 4, 7.5f, PhoneTheme.KEY_SUB);
        g.textC("\u25b2", DPAD_X, DPAD_Y - 36, 8f, PhoneTheme.KEY_SUB);
        g.textC("\u25bc", DPAD_X, DPAD_Y + 28, 8f, PhoneTheme.KEY_SUB);
        g.textC("\u25c0", DPAD_X - 32, DPAD_Y - 4, 8f, PhoneTheme.KEY_SUB);
        g.textC("\u25b6", DPAD_X + 32, DPAD_Y - 4, 8f, PhoneTheme.KEY_SUB);

        // ---- 12 number keys
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 3; c++) {
                int i = r * 3 + c;
                int x = KEY_COL[c];
                int y = KEY_ROW[r];
                boolean hover = mx >= x && mx < x + KEY_W && my >= y && my < y + KEY_H;
                g.rrect(x - 1, y - 1, KEY_W + 2, KEY_H + 2, 9, PhoneTheme.KEY_EDGE);
                g.rrect(x, y, KEY_W, KEY_H, 8, isPressed("k" + i) ? PhoneTheme.KEY_PRESS : hover ? PhoneTheme.KEY_HOVER : PhoneTheme.KEY);
                g.textC(KEY_MAIN[i], x + 28, y + 5, 14f, PhoneTheme.KEY_TEXT);
                g.text(KEY_KANA[i], x + 8, y + 22, 8f, PhoneTheme.KEY_SUB);
                g.textR(KEY_ABC[i], x + KEY_W - 8, y + 22, 8f, PhoneTheme.KEY_SUB);
            }
        }
    }

    /** Colours one quarter of the d-pad ring. */
    private void drawZone(PhoneGfx g, String zone, int color) {
        int cx = DPAD_X;
        int cy = DPAD_Y;
        int mid = (DPAD_R + DPAD_OK_R) / 2;
        switch (zone) {
            case "up": g.rrect(cx - 14, cy - DPAD_R + 4, 28, DPAD_R - DPAD_OK_R - 6, 6, color); break;
            case "down": g.rrect(cx - 14, cy + DPAD_OK_R + 2, 28, DPAD_R - DPAD_OK_R - 6, 6, color); break;
            case "left": g.rrect(cx - DPAD_R + 4, cy - 14, DPAD_R - DPAD_OK_R - 6, 28, 6, color); break;
            default: g.rrect(cx + DPAD_OK_R + 2, cy - 14, DPAD_R - DPAD_OK_R - 6, 28, 6, color); break;
        }
        if (mid < 0) return; // (keeps 'mid' used; no effect)
    }
}
