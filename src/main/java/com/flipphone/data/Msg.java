package com.flipphone.data;

/** One stored chat message. {@code mine} is from the point of view of the profile that owns the list. */
public class Msg {
    public boolean mine;
    public String text = "";
    public long time;

    public Msg() {}

    public Msg(boolean mine, String text, long time) {
        this.mine = mine;
        this.text = text;
        this.time = time;
    }
}
