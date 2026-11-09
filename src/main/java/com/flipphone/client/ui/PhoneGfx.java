package com.flipphone.client.ui;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * Drawing toolkit in "phone units": the coordinate system of the reference video (the whole phone is 312 x 828
 * units). The screen scales everything to the real window with one pose-stack transform, so views just draw in
 * units and never think about GUI scale.
 */
public final class PhoneGfx {
    private final GuiGraphics g;
    private final Font font;
    private final float scale;
    private final float originX;   // where phone unit (0,0) is on the real screen
    private final float originY;
    private int curX;              // extra offset of the current coordinate system, in units (for clipping)
    private int curY;

    public PhoneGfx(GuiGraphics g, Font font, float scale, float originX, float originY) {
        this.g = g;
        this.font = font;
        this.scale = scale;
        this.originX = originX;
        this.originY = originY;
    }

    public GuiGraphics graphics() {
        return g;
    }

    // ================================================================== coordinate system

    /** Moves the origin by (x, y) units until {@link #pop()}. */
    public void push(int x, int y) {
        g.pose().pushPose();
        g.pose().translate((float) x, (float) y, 0f);
        curX += x;
        curY += y;
        stackX[depth] = x;
        stackY[depth] = y;
        depth++;
    }

    public void pop() {
        depth--;
        curX -= stackX[depth];
        curY -= stackY[depth];
        g.pose().popPose();
    }

    private final int[] stackX = new int[16];
    private final int[] stackY = new int[16];
    private int depth;

    /** Only draws inside this rectangle (units, relative to the current origin) until {@link #unclip()}. */
    public void clip(int x, int y, int w, int h) {
        int x0 = Math.round(originX + (curX + x) * scale);
        int y0 = Math.round(originY + (curY + y) * scale);
        int x1 = Math.round(originX + (curX + x + w) * scale);
        int y1 = Math.round(originY + (curY + y + h) * scale);
        g.enableScissor(x0, y0, x1, y1);
    }

    public void unclip() {
        g.disableScissor();
    }

    // ================================================================== shapes

    public void fill(int x, int y, int w, int h, int argb) {
        g.fill(x, y, x + w, y + h, argb);
    }

    public void outline(int x, int y, int w, int h, int t, int argb) {
        fill(x, y, w, t, argb);
        fill(x, y + h - t, w, t, argb);
        fill(x, y + t, t, h - 2 * t, argb);
        fill(x + w - t, y + t, t, h - 2 * t, argb);
    }

    /** Rounded rectangle, one colour. */
    public void rrect(int x, int y, int w, int h, int r, int argb) {
        rrectGrad(x, y, w, h, r, argb, argb);
    }

    /** Rounded rectangle with a vertical colour gradient (top -> bottom). */
    public void rrectGrad(int x, int y, int w, int h, int r, int top, int bottom) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        boolean solid = top == bottom;
        int step = solid ? 1 : 3;
        int y0 = 0;
        while (y0 < h) {
            int rowH;
            int inset = 0;
            boolean inCorner = y0 < r || y0 >= h - r;
            if (solid && !inCorner) {
                rowH = Math.max(1, h - r - y0); // the whole straight middle part in one fill
            } else {
                rowH = inCorner && solid ? 1 : Math.min(step, h - y0);
            }
            if (inCorner) {
                float cy = y0 + rowH / 2f;
                float edge = Math.min(cy, h - cy);
                inset = edge >= r ? 0 : (int) Math.round(r - Math.sqrt(Math.max(0, r * r - (r - edge) * (r - edge))));
            }
            int color = solid ? top : lerp(top, bottom, (y0 + rowH / 2f) / h);
            if (w - 2 * inset > 0) fill(x + inset, y + y0, w - 2 * inset, Math.min(rowH, h - y0), color);
            y0 += rowH;
        }
    }

    public void circle(int cx, int cy, int radius, int argb) {
        rrect(cx - radius, cy - radius, radius * 2, radius * 2, radius, argb);
    }

    private static int lerp(int a, int b, float t) {
        int aa = a >>> 24, ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255;
        int ba = b >>> 24, br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    // ================================================================== text (size = height of the text line in units)

    public float width(String s, float size) {
        return font.width(s) * size / 9f;
    }

    public void text(String s, float x, float y, float size, int color) {
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(x, y, 0f);
        float k = size / 9f;
        ps.scale(k, k, 1f);
        g.drawString(font, s, 0, 0, color, false);
        ps.popPose();
    }

    public void textBold(String s, float x, float y, float size, int color) {
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(x, y, 0f);
        float k = size / 9f;
        ps.scale(k, k, 1f);
        g.drawString(font, Component.literal(s).withStyle(ChatFormatting.BOLD), 0, 0, color, false);
        ps.popPose();
    }

    public void textC(String s, float cx, float y, float size, int color) {
        text(s, cx - width(s, size) / 2f, y, size, color);
    }

    public void textR(String s, float rx, float y, float size, int color) {
        text(s, rx - width(s, size), y, size, color);
    }

    /** Shortens a string with ".." so it fits {@code maxW} units. */
    public String fit(String s, float size, float maxW) {
        if (width(s, size) <= maxW) return s;
        String t = s;
        while (!t.isEmpty() && width(t + "..", size) > maxW) t = t.substring(0, t.length() - 1);
        return t + "..";
    }

    /** Word-wraps a string to {@code maxW} units. */
    public List<FormattedCharSequence> wrap(String s, float size, int maxW) {
        return font.split(Component.literal(s), Math.max(8, (int) (maxW * 9f / size)));
    }

    public void drawSeq(FormattedCharSequence seq, float x, float y, float size, int color) {
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(x, y, 0f);
        float k = size / 9f;
        ps.scale(k, k, 1f);
        g.drawString(font, seq, 0, 0, color, false);
        ps.popPose();
    }
}
