package com.flipphone.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Everything the phone stores for one player. Plain fields so Gson can read and write it. */
public class PhoneProfile {
    /** Permanent phone number, e.g. 090-1234-5678. */
    public String number = "";
    /** Last Minecraft username seen (informational). */
    public String lastName = "";
    /** Yen balance. */
    public long yen;
    /** The permanent YenApp wallet tag ("" = not registered yet). */
    public String tag = "";
    /** number -> nickname */
    public Map<String, String> contacts = new LinkedHashMap<>();
    /** number -> messages; the most recently used conversation is LAST. */
    public Map<String, List<Msg>> chats = new LinkedHashMap<>();
    public int messagesSent;
    public int messagesReceived;
    public int calls;
    public long callSeconds;
    /** Newest first. */
    public List<CallRec> recent = new ArrayList<>();

    /** Repairs anything missing after loading an older or hand-edited file. */
    public PhoneProfile sanitize() {
        if (number == null) number = "";
        if (lastName == null) lastName = "";
        if (tag == null) tag = "";
        if (contacts == null) contacts = new LinkedHashMap<>();
        if (chats == null) chats = new LinkedHashMap<>();
        if (recent == null) recent = new ArrayList<>();
        if (yen < 0) yen = 0;
        return this;
    }
}
