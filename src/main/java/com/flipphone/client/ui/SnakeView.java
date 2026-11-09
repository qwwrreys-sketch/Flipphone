package com.flipphone.client.ui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Snake (video): a 15 x 15 grid, the snake is black/grey, the food is red. Eat food to grow (score and level go
 * up and it speeds up); hitting a wall or your own tail ends the game with a red wipe, then OK = retry.
 * The best score is kept in config/flipphone_snake_best.txt.
 */
public final class SnakeView extends View {
    private static final int N = 15;
    private static final int CELL = 16;
    private static final int GX = 13;
    private static final int GY = 64;

    private enum State { SPLASH, PLAY, PAUSED, WIPE, OVER }

    private static int best = loadBest();

    private State state = State.SPLASH;
    private final long splashUntil = System.currentTimeMillis() + 900;
    private final Random rng = new Random();
    private final int[] sx = new int[N * N];
    private final int[] sy = new int[N * N];
    private int len;
    private int dir;          // 0 up, 1 right, 2 down, 3 left
    private int nextDir;
    private int queuedDir = -1;
    private int fx;
    private int fy;
    private int score;
    private int ticks;
    private int wipe;

    public SnakeView(PhoneScreen ph) {
        super(ph);
    }

    @Override
    public String left() {
        return state == State.OVER ? "QUIT" : "BACK";
    }

    @Override
    public String center() {
        switch (state) {
            case PLAY: return "PAUSE";
            case PAUSED: return "RESUME";
            case OVER: return "RETRY";
            default: return "";
        }
    }

    @Override public String right() { return ""; }

    private int level() {
        return Math.min(10, score + 1);
    }

    private void reset() {
        len = 3;
        for (int i = 0; i < len; i++) {
            sx[i] = N / 2 - i;
            sy[i] = N / 2;
        }
        dir = 1;
        nextDir = 1;
        queuedDir = -1;
        score = 0;
        ticks = 0;
        wipe = 0;
        placeFood();
        state = State.PLAY;
    }

    private void placeFood() {
        for (int tries = 0; tries < 500; tries++) {
            int x = rng.nextInt(N);
            int y = rng.nextInt(N);
            boolean free = true;
            for (int i = 0; i < len; i++) if (sx[i] == x && sy[i] == y) free = false;
            if (free) {
                fx = x;
                fy = y;
                return;
            }
        }
    }

    @Override
    public void tick() {
        if (state == State.SPLASH && System.currentTimeMillis() >= splashUntil) reset();
        if (state == State.WIPE) {
            if (++wipe > N) state = State.OVER;
            return;
        }
        if (state != State.PLAY) return;
        int interval = Math.max(4, 8 - (level() - 1) / 2); // ticks per step: faster with every level
        if (++ticks < interval) return;
        ticks = 0;
        step();
    }

    private void step() {
        dir = nextDir;
        if (queuedDir >= 0) {
            nextDir = queuedDir;
            queuedDir = -1;
        }
        int hx = sx[0] + (dir == 1 ? 1 : dir == 3 ? -1 : 0);
        int hy = sy[0] + (dir == 2 ? 1 : dir == 0 ? -1 : 0);
        boolean eats = hx == fx && hy == fy;
        if (hx < 0 || hy < 0 || hx >= N || hy >= N) {
            gameOver();
            return;
        }
        int bodyLen = eats ? len : len - 1; // the tail tip moves away unless we grow
        for (int i = 0; i < bodyLen; i++) {
            if (sx[i] == hx && sy[i] == hy) {
                gameOver();
                return;
            }
        }
        if (eats) len = Math.min(len + 1, N * N);
        for (int i = len - 1; i > 0; i--) {
            sx[i] = sx[i - 1];
            sy[i] = sy[i - 1];
        }
        sx[0] = hx;
        sy[0] = hy;
        if (eats) {
            score++;
            if (score > best) best = score;
            placeFood();
        }
    }

    private void gameOver() {
        state = State.WIPE;
        wipe = 0;
        saveBest();
    }

    @Override
    public void render(PhoneGfx g, int mx, int my) {
        Widgets.header(g, "SNAKE");
        if (state == State.SPLASH) {
            g.rrect(100, 120, 66, 66, 12, 0xFFE9EBDD);
            g.fill(113, 133, 10, 10, 0xFF3E403A);
            g.fill(113, 143, 10, 10, 0xFF3E403A);
            g.fill(123, 153, 10, 10, 0xFF3E403A);
            g.fill(133, 153, 10, 10, 0xFF3E403A);
            g.fill(143, 143, 5, 5, 0xFFDF3322);
            return;
        }
        String hint = state == State.OVER || state == State.WIPE ? "GAME OVER   OK: RETRY OR QUIT"
                : "LVL " + level() + "   \u25c0\u25b2\u25bc\u25b6: MOVE   PAUSE: OK";
        g.textC(hint, 133, 32, 7.5f, PhoneTheme.MUTED);
        g.text("SCORE: " + score + "   BEST: " + best, 8, 46, 9f, PhoneTheme.INK_SOFT);

        // grid
        g.rrect(GX - 3, GY - 3, N * CELL + 6, N * CELL + 6, 8, PhoneTheme.INK_SOFT);
        g.fill(GX, GY, N * CELL, N * CELL, PhoneTheme.SNAKE_BG);
        for (int i = 0; i <= N; i++) {
            g.fill(GX + i * CELL, GY, 1, N * CELL, PhoneTheme.SNAKE_GRID);
            g.fill(GX, GY + i * CELL, N * CELL, 1, PhoneTheme.SNAKE_GRID);
        }
        // food
        g.fill(GX + fx * CELL + 1, GY + fy * CELL + 1, CELL - 1, CELL - 1, PhoneTheme.SNAKE_FOOD);
        // snake (head darkest)
        for (int i = len - 1; i >= 0; i--) {
            g.fill(GX + sx[i] * CELL + 1, GY + sy[i] * CELL + 1, CELL - 1, CELL - 1, i == 0 ? PhoneTheme.SNAKE_HEAD : PhoneTheme.SNAKE_BODY);
        }
        // red wipe after the game is lost
        if (state == State.WIPE || state == State.OVER) {
            int rows = state == State.OVER ? N : Math.min(N, wipe);
            g.fill(GX, GY, N * CELL, rows * CELL, 0xFFE8604C);
            for (int i = 0; i <= rows; i++) g.fill(GX, GY + i * CELL, N * CELL, 1, 0xFFEF7A68);
            if (state == State.OVER) g.textC("GAME OVER", GX + N * CELL / 2f, GY + N * CELL / 2f - 8, 16f, PhoneTheme.WHITE);
        }
    }

    private void turn(int d) {
        if (state != State.PLAY) return;
        int cur = queuedDir >= 0 ? queuedDir : nextDir;
        if (d == cur || (d + 2) % 4 == cur) return; // no reversing into yourself
        if (nextDir == dir) nextDir = d;
        else queuedDir = d;
    }

    @Override
    public boolean onKey(Key k) {
        switch (k) {
            case UP: turn(0); return true;
            case RIGHT: turn(1); return true;
            case DOWN: turn(2); return true;
            case LEFT: turn(3); return true;
            case OK:
                if (state == State.PLAY) state = State.PAUSED;
                else if (state == State.PAUSED) state = State.PLAY;
                else if (state == State.OVER) reset();
                return true;
            case MENU: return super.onKey(k);
            default: return super.onKey(k);
        }
    }

    // ---- best score on disk
    private static Path bestFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("flipphone_snake_best.txt");
    }

    private static int loadBest() {
        try {
            Path f = bestFile();
            if (Files.isRegularFile(f)) return Math.max(0, Integer.parseInt(Files.readString(f).trim()));
        } catch (IOException | RuntimeException ignored) {
            // no best score yet
        }
        return 0;
    }

    private static void saveBest() {
        try {
            Files.writeString(bestFile(), Integer.toString(best));
        } catch (IOException ignored) {
            // not important
        }
    }
}
