package com.flipphone.client.ui;

/** One screen of the phone (home, menu, LINE ...). The phone keeps a stack of them. */
public abstract class View {
    protected final PhoneScreen ph;

    protected View(PhoneScreen ph) {
        this.ph = ph;
    }

    /** Labels of the three soft keys under the LCD ("" = nothing). */
    public abstract String left();

    public abstract String center();

    public abstract String right();

    /** Draws the content area (266 x 324 units). Mouse position is relative to that area. */
    public abstract void render(PhoneGfx g, int mx, int my);

    /** @return true when the key was used */
    public boolean onKey(Key k) {
        switch (k) {
            case BACK:
            case LEFT_SOFT:
                ph.pop();
                return true;
            case MENU:
                ph.openMenu();
                return true;
            default:
                return false;
        }
    }

    /** A typed character or a keypad digit. */
    public void onChar(char c) {
    }

    /** Mouse click inside the content area. */
    public boolean onClick(int x, int y) {
        return false;
    }

    public void tick() {
    }

    /** The view became the visible one (pushed, or returned to). */
    public void onShow() {
    }

    /** The view was removed (popped, or the phone closed). */
    public void onLeave() {
    }

    /** true while a text box is active (letters go into it, M does not open the menu). */
    public boolean textMode() {
        return false;
    }

    /** true = drawn on top of the view below it (popup menus). */
    public boolean isOverlay() {
        return false;
    }
}
