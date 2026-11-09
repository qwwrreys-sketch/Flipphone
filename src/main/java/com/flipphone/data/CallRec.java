package com.flipphone.data;

/** One line of the "Recent calls" list. */
public class CallRec {
    public boolean outgoing;
    public String number = "";
    /** "Hang up", "Call Denied", "Missed", "No answer", "Unreachable" ... */
    public String outcome = "";
    public long time;
    public int seconds;

    public CallRec() {}

    public CallRec(boolean outgoing, String number, String outcome, long time, int seconds) {
        this.outgoing = outgoing;
        this.number = number;
        this.outcome = outcome;
        this.time = time;
        this.seconds = seconds;
    }
}
