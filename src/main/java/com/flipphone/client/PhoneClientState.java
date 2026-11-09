package com.flipphone.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.flipphone.net.PhoneNet;

/** Everything the phone UI knows. Only touched from the client thread. */
public final class PhoneClientState {
    private PhoneClientState() {}

    /** The last full state from the server (null until the first packet arrives). */
    public static PhoneNet.StateDto state;
    /** Counts up on every change so views can tell something happened. */
    public static int version;

    /** number -> chat history (as last received / appended). */
    public static final Map<String, List<PhoneNet.MsgDto>> history = new HashMap<>();

    // ---------------------------------------------------------------- call
    public static final int CALL_NONE = -1;
    public static int callPhase = CALL_NONE;       // CALL_NONE or one of PhoneNet.EVT_*
    public static String callNumber = "";
    public static long callActiveSince;            // client millis when the call became active
    public static String callEndReason = "";
    public static int callEndSeconds;
    public static long callEndedAt;

    // ---------------------------------------------------------------- yen
    public record YenResult(int op, boolean ok, String message, String tag, long at) {}

    public static YenResult lastYen;

    // ---------------------------------------------------------------- social
    public static List<String> social = new ArrayList<>();
    public static long socialAt;

    // ---------------------------------------------------------------- toast
    public static String notice = "";
    public static long noticeUntil;

    // ================================================================== updates from the network

    public static void applyState(PhoneNet.StateDto s) {
        state = s;
        version++;
    }

    public static void applyHistory(String number, List<PhoneNet.MsgDto> list) {
        history.put(number, new ArrayList<>(list));
        version++;
    }

    public static void applyMessage(String number, PhoneNet.MsgDto m) {
        history.computeIfAbsent(number, k -> new ArrayList<>()).add(m);
        version++;
    }

    public static void applyCall(int evt, String number, int seconds, String reason) {
        callPhase = evt;
        if (!number.isEmpty()) callNumber = number;
        if (evt == PhoneNet.EVT_ACTIVE) callActiveSince = System.currentTimeMillis();
        if (evt == PhoneNet.EVT_ENDED) {
            callEndReason = reason;
            callEndSeconds = seconds;
            callEndedAt = System.currentTimeMillis();
        }
        version++;
    }

    public static void applyYen(int op, boolean ok, String message, String tag) {
        lastYen = new YenResult(op, ok, message, tag, System.currentTimeMillis());
        version++;
    }

    public static void applySocial(List<String> list) {
        social = list;
        socialAt = System.currentTimeMillis();
        version++;
    }

    public static void applyNotice(String text) {
        notice = text;
        noticeUntil = System.currentTimeMillis() + 3200;
        version++;
    }

    public static void reset() {
        state = null;
        history.clear();
        callPhase = CALL_NONE;
        callNumber = "";
        lastYen = null;
        social = new ArrayList<>();
        notice = "";
        version++;
    }

    // ================================================================== helpers for the views

    public static boolean inCall() {
        return callPhase == PhoneNet.EVT_RINGING_OUT || callPhase == PhoneNet.EVT_INCOMING || callPhase == PhoneNet.EVT_ACTIVE;
    }

    public static boolean isContact(String number) {
        if (state == null) return false;
        for (PhoneNet.ContactDto c : state.contacts()) if (c.number().equals(number)) return true;
        return false;
    }

    /** The saved nickname of a number, or the number itself. */
    public static String nameOf(String number) {
        if (state != null) for (PhoneNet.ContactDto c : state.contacts()) if (c.number().equals(number)) return c.name();
        return number;
    }
}
