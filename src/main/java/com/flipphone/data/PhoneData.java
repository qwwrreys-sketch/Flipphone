package com.flipphone.data;

import java.util.HashMap;
import java.util.Map;

/** The whole save file (world/flipphone/phones.json). */
public class PhoneData {
    /** player uuid -> profile */
    public Map<String, PhoneProfile> players = new HashMap<>();

    public PhoneData sanitize() {
        if (players == null) players = new HashMap<>();
        players.values().removeIf(p -> p == null);
        for (PhoneProfile p : players.values()) p.sanitize();
        return this;
    }

    public PhoneProfile profile(String uuid) {
        return players.computeIfAbsent(uuid, k -> new PhoneProfile());
    }
}
