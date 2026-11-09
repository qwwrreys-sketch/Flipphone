package com.flipphone.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import com.flipphone.data.CallRec;
import com.flipphone.data.Msg;
import com.flipphone.data.PhoneData;
import com.flipphone.data.PhoneProfile;
import com.flipphone.data.PhoneStore;
import com.flipphone.net.PhoneNet;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * All server-side logic of the phone. Everything the client sends is validated here again: the phone UI is only
 * a convenience, the server decides. Runs on the server thread.
 *
 * <ul>
 *   <li>phone numbers (random 090-XXXX-XXXX, permanent; one VIP number for one exact username)</li>
 *   <li>LINE messages, contacts</li>
 *   <li>calls (ringing / answering / hanging up / recent calls)</li>
 *   <li>YenApp (permanent tag, lookup, payments)</li>
 *   <li>SocialLink (who is within 5 blocks and has the app open)</li>
 * </ul>
 */
public final class PhoneService {
    private PhoneService() {}

    // =====================================================================================
    //  VIP: this exact username ALWAYS gets this number (case-sensitive comparison).
    // =====================================================================================
    public static final String VIP_USERNAME = "BaXzuXka";
    public static final String VIP_NUMBER = "555-5555-5555";

    private static final long RING_TIMEOUT_MS = 30_000;
    private static final long SOCIAL_FRESH_MS = 4_000;
    private static final long SEND_COOLDOWN_MS = 350;
    private static final int MAX_CONTACTS = 100;
    private static final int MAX_MESSAGES_PER_CHAT = 300;
    private static final int MAX_RECENT_CALLS = 30;

    private static MinecraftServer server;
    private static PhoneData data = new PhoneData();
    private static boolean dirty;
    private static int saveTimer;

    private static final Map<String, UUID> BY_NUMBER = new HashMap<>();
    private static final Map<String, UUID> BY_TAG = new HashMap<>(); // lower-case tag -> player
    private static final Map<UUID, Long> SOCIAL_OPEN = new HashMap<>();
    private static final Map<UUID, Long> LAST_SEND = new HashMap<>();
    private static final Map<UUID, Call> CALLS = new HashMap<>();    // both participants point to the same Call
    private static final Random RNG = new Random();

    private static final class Call {
        final UUID caller;
        final UUID callee;
        final long ringStart = System.currentTimeMillis();
        boolean active;
        long startMs;

        Call(UUID caller, UUID callee) {
            this.caller = caller;
            this.callee = callee;
        }
    }

    // ================================================================== setup

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(s -> {
            server = s;
            data = PhoneStore.load(s);
            rebuildIndexes();
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(s -> {
            for (Call c : new HashSet<>(CALLS.values())) endCall(c, null, 2);
            if (server != null) PhoneStore.save(server, data);
            CALLS.clear();
            SOCIAL_OPEN.clear();
            LAST_SEND.clear();
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, s) -> onJoin(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, s) -> onLeave(handler.player));
        ServerTickEvents.END_SERVER_TICK.register(PhoneService::tick);

        // decode on the network thread, act on the server thread
        ServerPlayNetworking.registerGlobalReceiver(PhoneNet.C2S_REQUEST,
                (srv, player, handler, buf, sender) -> srv.execute(() -> sendState(player)));
        ServerPlayNetworking.registerGlobalReceiver(PhoneNet.C2S_CHAT_OPEN, (srv, player, handler, buf, sender) -> {
            String number = buf.readUtf(32);
            srv.execute(() -> sendHistory(player, number));
        });
        ServerPlayNetworking.registerGlobalReceiver(PhoneNet.C2S_SEND, (srv, player, handler, buf, sender) -> {
            String number = buf.readUtf(32);
            String text = buf.readUtf(512);
            srv.execute(() -> sendMessage(player, number, text));
        });
        ServerPlayNetworking.registerGlobalReceiver(PhoneNet.C2S_CONTACT, (srv, player, handler, buf, sender) -> {
            int op = buf.readVarInt();
            String number = buf.readUtf(32);
            String nick = buf.readUtf(64);
            boolean viaSocial = buf.readBoolean();
            srv.execute(() -> contact(player, op, number, nick, viaSocial));
        });
        ServerPlayNetworking.registerGlobalReceiver(PhoneNet.C2S_DIAL, (srv, player, handler, buf, sender) -> {
            String number = buf.readUtf(32);
            srv.execute(() -> dial(player, number));
        });
        ServerPlayNetworking.registerGlobalReceiver(PhoneNet.C2S_CALL, (srv, player, handler, buf, sender) -> {
            int action = buf.readVarInt();
            srv.execute(() -> callAction(player, action));
        });
        ServerPlayNetworking.registerGlobalReceiver(PhoneNet.C2S_YEN, (srv, player, handler, buf, sender) -> {
            int op = buf.readVarInt();
            String arg = buf.readUtf(64);
            int amount = buf.readVarInt();
            srv.execute(() -> yen(player, op, arg, amount));
        });
        ServerPlayNetworking.registerGlobalReceiver(PhoneNet.C2S_SOCIAL, (srv, player, handler, buf, sender) -> {
            boolean open = buf.readBoolean();
            srv.execute(() -> social(player, open));
        });
    }

    private static void rebuildIndexes() {
        BY_NUMBER.clear();
        BY_TAG.clear();
        for (Map.Entry<String, PhoneProfile> e : data.players.entrySet()) {
            UUID id;
            try {
                id = UUID.fromString(e.getKey());
            } catch (IllegalArgumentException bad) {
                continue;
            }
            PhoneProfile p = e.getValue();
            if (!p.number.isEmpty()) BY_NUMBER.put(p.number, id);
            if (!p.tag.isEmpty()) BY_TAG.put(p.tag.toLowerCase(Locale.ROOT), id);
        }
    }

    private static void tick(MinecraftServer s) {
        if (++saveTimer >= 600) { // every 30 s: save if something changed
            saveTimer = 0;
            if (dirty) saveNow();
        }
        if (s.getTickCount() % 10 == 0 && !CALLS.isEmpty()) {
            long now = System.currentTimeMillis();
            for (Call c : new HashSet<>(CALLS.values())) {
                if (!c.active && now - c.ringStart > RING_TIMEOUT_MS) endCall(c, null, 1);
            }
        }
    }

    private static void saveNow() {
        if (server != null) PhoneStore.save(server, data);
        dirty = false;
    }

    private static PhoneProfile profile(ServerPlayer p) {
        return data.profile(p.getUUID().toString());
    }

    private static PhoneProfile profile(UUID id) {
        return data.profile(id.toString());
    }

    // ================================================================== join / leave / numbers

    private static void onJoin(ServerPlayer p) {
        if (server == null) return;
        UUID id = p.getUUID();
        PhoneProfile pr = profile(p);
        String name = p.getGameProfile().getName();
        pr.lastName = name;
        if (pr.number.isEmpty()) setNumber(id, pr, freshNumber());
        // the VIP username always owns the VIP number (unless someone else somehow holds it)
        if (VIP_USERNAME.equals(name) && !VIP_NUMBER.equals(pr.number)) {
            UUID holder = BY_NUMBER.get(VIP_NUMBER);
            if (holder == null || holder.equals(id)) setNumber(id, pr, VIP_NUMBER);
        }
        dirty = true;
        sendState(p);
    }

    private static void onLeave(ServerPlayer p) {
        UUID id = p.getUUID();
        Call c = CALLS.get(id);
        if (c != null) endCall(c, id, 2);
        SOCIAL_OPEN.remove(id);
        LAST_SEND.remove(id);
        if (dirty) saveNow();
    }

    private static void setNumber(UUID id, PhoneProfile pr, String number) {
        if (!pr.number.isEmpty()) BY_NUMBER.remove(pr.number, id);
        pr.number = number;
        BY_NUMBER.put(number, id);
    }

    /** A random 090-XXXX-XXXX that nobody has yet. */
    private static String freshNumber() {
        for (;;) {
            String n = String.format("090-%04d-%04d", RNG.nextInt(10000), RNG.nextInt(10000));
            if (!BY_NUMBER.containsKey(n)) return n;
        }
    }

    /** "09012345678" / "090-1234-5678" -> "090-1234-5678"; null when it is not an 11-digit number. */
    public static String normNumber(String raw) {
        if (raw == null) return null;
        String d = raw.replaceAll("[^0-9]", "");
        if (d.length() != 11) return null;
        return d.substring(0, 3) + "-" + d.substring(3, 7) + "-" + d.substring(7);
    }

    // ================================================================== state packets

    private static void sendState(ServerPlayer p) {
        PhoneProfile pr = profile(p);
        List<PhoneNet.ContactDto> contacts = new ArrayList<>();
        for (Map.Entry<String, String> e : pr.contacts.entrySet()) contacts.add(new PhoneNet.ContactDto(e.getKey(), e.getValue()));

        List<PhoneNet.ChatDto> chats = new ArrayList<>();
        List<Map.Entry<String, List<Msg>>> entries = new ArrayList<>(pr.chats.entrySet());
        for (int i = entries.size() - 1; i >= 0 && chats.size() < 50; i--) { // newest conversation first
            List<Msg> l = entries.get(i).getValue();
            if (l == null || l.isEmpty()) continue;
            Msg last = l.get(l.size() - 1);
            chats.add(new PhoneNet.ChatDto(entries.get(i).getKey(), last.text, last.mine, last.time));
        }

        List<PhoneNet.CallDto> recent = new ArrayList<>();
        for (CallRec r : pr.recent) recent.add(new PhoneNet.CallDto(r.outgoing, r.number, r.outcome, r.time, r.seconds));

        FriendlyByteBuf buf = PacketByteBufs.create();
        new PhoneNet.StateDto(pr.number, pr.yen, pr.tag, pr.messagesSent, pr.messagesReceived, pr.calls, pr.callSeconds,
                contacts, chats, recent).write(buf);
        ServerPlayNetworking.send(p, PhoneNet.S2C_STATE, buf);
    }

    private static void sendHistory(ServerPlayer p, String numberRaw) {
        String number = normNumber(numberRaw);
        if (number == null) return;
        List<Msg> list = profile(p).chats.get(number);
        int from = list == null ? 0 : Math.max(0, list.size() - 100);
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUtf(number, 32);
        buf.writeVarInt(list == null ? 0 : list.size() - from);
        if (list != null) for (int i = from; i < list.size(); i++) new PhoneNet.MsgDto(list.get(i).mine, list.get(i).text, list.get(i).time).write(buf);
        ServerPlayNetworking.send(p, PhoneNet.S2C_HISTORY, buf);
    }

    private static void notice(ServerPlayer p, String text) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUtf(text, 200);
        ServerPlayNetworking.send(p, PhoneNet.S2C_NOTICE, buf);
    }

    /** Pushes a fresh state to a player if (and only if) they are online. Used by commands. */
    public static void refresh(UUID id) {
        if (server == null) return;
        ServerPlayer p = server.getPlayerList().getPlayer(id);
        if (p != null) sendState(p);
    }

    // ================================================================== messages and contacts

    private static String clean(String s, int max) {
        if (s == null) return "";
        String t = s.replaceAll("\\p{Cntrl}", "").trim();
        return t.length() > max ? t.substring(0, max) : t;
    }

    private static void addMsg(PhoneProfile owner, String other, Msg m) {
        List<Msg> list = owner.chats.remove(other);
        if (list == null) list = new ArrayList<>();
        list.add(m);
        while (list.size() > MAX_MESSAGES_PER_CHAT) list.remove(0);
        owner.chats.put(other, list); // re-inserted last = most recent conversation
    }

    private static void sendMessage(ServerPlayer from, String numberRaw, String textRaw) {
        String number = normNumber(numberRaw);
        String text = clean(textRaw, PhoneNet.MSG_MAX);
        if (number == null || text.isEmpty()) return;

        long now = System.currentTimeMillis();
        Long last = LAST_SEND.get(from.getUUID());
        if (last != null && now - last < SEND_COOLDOWN_MS) return; // anti-spam
        LAST_SEND.put(from.getUUID(), now);

        UUID toId = BY_NUMBER.get(number);
        if (toId == null) {
            notice(from, "That number is not in service.");
            return;
        }
        if (toId.equals(from.getUUID())) {
            notice(from, "You can't message yourself.");
            return;
        }
        PhoneProfile a = profile(from);
        PhoneProfile b = profile(toId);
        if (!a.contacts.containsKey(number) && !a.chats.containsKey(number)) {
            notice(from, "Save this number as a contact first.");
            return;
        }

        addMsg(a, number, new Msg(true, text, now));
        a.messagesSent++;
        addMsg(b, a.number, new Msg(false, text, now));
        b.messagesReceived++;
        dirty = true;

        sendState(from);
        sendMessageEvent(from, number, new PhoneNet.MsgDto(true, text, now));
        ServerPlayer to = server.getPlayerList().getPlayer(toId);
        if (to != null) {
            sendState(to);
            sendMessageEvent(to, a.number, new PhoneNet.MsgDto(false, text, now));
        }
    }

    private static void sendMessageEvent(ServerPlayer to, String otherNumber, PhoneNet.MsgDto m) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUtf(otherNumber, 32);
        m.write(buf);
        ServerPlayNetworking.send(to, PhoneNet.S2C_MESSAGE, buf);
    }

    private static boolean knownNumber(PhoneProfile a, String number) {
        if (a.chats.containsKey(number)) return true;
        for (CallRec r : a.recent) if (r.number.equals(number)) return true;
        return false;
    }

    /** Is the owner of {@code number} within SocialLink range of {@code p} and showing the SocialLink app? */
    private static boolean nearbyAndOpen(ServerPlayer p, String number) {
        UUID id = BY_NUMBER.get(number);
        if (id == null || server == null) return false;
        ServerPlayer o = server.getPlayerList().getPlayer(id);
        Long t = SOCIAL_OPEN.get(id);
        if (o == null || o == p || t == null || System.currentTimeMillis() - t > SOCIAL_FRESH_MS) return false;
        return o.level() == p.level() && p.distanceTo(o) <= PhoneNet.SOCIAL_RANGE + 1.0;
    }

    private static void contact(ServerPlayer p, int op, String numberRaw, String nickRaw, boolean viaSocial) {
        String number = normNumber(numberRaw);
        if (number == null) return;
        PhoneProfile a = profile(p);

        if (op == PhoneNet.CONTACT_DELETE) {
            if (a.contacts.remove(number) != null) {
                dirty = true;
                sendState(p);
                notice(p, "Contact deleted.");
            }
            return;
        }

        String nick = clean(nickRaw, PhoneNet.NICK_MAX);
        if (nick.isEmpty()) {
            notice(p, "Enter a name first.");
            return;
        }
        if (op == PhoneNet.CONTACT_RENAME) {
            if (a.contacts.containsKey(number)) {
                a.contacts.put(number, nick);
                dirty = true;
                sendState(p);
                notice(p, "Contact renamed.");
            }
            return;
        }

        // ADD
        if (!BY_NUMBER.containsKey(number) || number.equals(a.number)) {
            notice(p, "That number can't be saved.");
            return;
        }
        if (a.contacts.size() >= MAX_CONTACTS && !a.contacts.containsKey(number)) {
            notice(p, "Your contact list is full.");
            return;
        }
        boolean allowed = viaSocial ? nearbyAndOpen(p, number) : knownNumber(a, number);
        if (!allowed) {
            notice(p, viaSocial ? "That person is no longer nearby." : "Use SocialLink to get a new number.");
            return;
        }
        a.contacts.put(number, nick);
        dirty = true;
        sendState(p);
        notice(p, "Contact saved.");
    }

    // ================================================================== SocialLink

    private static void social(ServerPlayer p, boolean open) {
        UUID id = p.getUUID();
        if (!open) {
            SOCIAL_OPEN.remove(id);
            return;
        }
        SOCIAL_OPEN.put(id, System.currentTimeMillis());
        List<String> found = new ArrayList<>();
        for (ServerPlayer o : server.getPlayerList().getPlayers()) {
            if (o == p) continue;
            String number = profile(o).number;
            if (!number.isEmpty() && nearbyAndOpen(p, number)) found.add(number);
        }
        FriendlyByteBuf buf = PacketByteBufs.create();
        PhoneNet.writeStrings(buf, found);
        ServerPlayNetworking.send(p, PhoneNet.S2C_SOCIAL, buf);
    }

    // ================================================================== calls

    private static void callEvent(ServerPlayer p, int evt, String number, int seconds, String reason) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(evt);
        buf.writeUtf(number, 32);
        buf.writeVarInt(seconds);
        buf.writeUtf(reason, 64);
        ServerPlayNetworking.send(p, PhoneNet.S2C_CALL, buf);
    }

    private static void addRecent(PhoneProfile pr, CallRec rec) {
        pr.recent.add(0, rec);
        while (pr.recent.size() > MAX_RECENT_CALLS) pr.recent.remove(pr.recent.size() - 1);
    }

    private static void dial(ServerPlayer p, String numberRaw) {
        String number = normNumber(numberRaw);
        UUID id = p.getUUID();
        if (CALLS.containsKey(id)) return; // already busy
        if (number == null) {
            callEvent(p, PhoneNet.EVT_ENDED, "", 0, "INVALID NUMBER");
            return;
        }
        UUID toId = BY_NUMBER.get(number);
        if (toId == null || toId.equals(id)) {
            callEvent(p, PhoneNet.EVT_ENDED, number, 0, toId == null ? "NOT IN SERVICE" : "INVALID NUMBER");
            return;
        }
        PhoneProfile a = profile(p);
        ServerPlayer to = server.getPlayerList().getPlayer(toId);
        a.calls++;
        long now = System.currentTimeMillis();
        if (to == null || CALLS.containsKey(toId)) {
            String why = to == null ? "OUT OF SERVICE AREA" : "LINE BUSY";
            addRecent(a, new CallRec(true, number, to == null ? "Unreachable" : "Busy", now, 0));
            dirty = true;
            sendState(p);
            callEvent(p, PhoneNet.EVT_ENDED, number, 0, why);
            return;
        }
        Call c = new Call(id, toId);
        CALLS.put(id, c);
        CALLS.put(toId, c);
        callEvent(p, PhoneNet.EVT_RINGING_OUT, number, 0, "");
        callEvent(to, PhoneNet.EVT_INCOMING, a.number, 0, "");
    }

    private static void callAction(ServerPlayer p, int action) {
        UUID id = p.getUUID();
        Call c = CALLS.get(id);
        if (c == null) return;
        if (action == PhoneNet.CALL_ANSWER) {
            if (c.caller.equals(id) || c.active) return; // only the callee can answer, once
            c.active = true;
            c.startMs = System.currentTimeMillis();
            ServerPlayer caller = server.getPlayerList().getPlayer(c.caller);
            if (caller != null) callEvent(caller, PhoneNet.EVT_ACTIVE, profile(c.callee).number, 0, "");
            callEvent(p, PhoneNet.EVT_ACTIVE, profile(c.caller).number, 0, "");
        } else {
            endCall(c, id, 0);
        }
    }

    /**
     * Ends a call and writes both players' recent-call lists.
     *
     * @param ender who ended it (null = nobody: timeout / server stop)
     * @param why   0 = a player hung up / denied, 1 = nobody answered, 2 = disconnect / server stop
     */
    private static void endCall(Call c, UUID ender, int why) {
        CALLS.remove(c.caller);
        CALLS.remove(c.callee);
        PhoneProfile a = profile(c.caller);
        PhoneProfile b = profile(c.callee);
        long now = System.currentTimeMillis();
        int secs = c.active ? (int) Math.max(0, (now - c.startMs) / 1000) : 0;

        String outA;
        String outB;
        if (c.active) {
            outA = "Hang up";
            outB = "Hang up";
            a.callSeconds += secs;
            b.callSeconds += secs;
        } else if (why == 1) {
            outA = "No answer";
            outB = "Missed";
        } else if (c.caller.equals(ender) || why == 2 && !c.callee.equals(ender)) {
            outA = "Hang up";   // the caller gave up before it was answered
            outB = "Missed";
        } else {
            outA = "Call Denied";
            outB = "Call Denied";
        }
        addRecent(a, new CallRec(true, b.number, outA, now, secs));
        addRecent(b, new CallRec(false, a.number, outB, now, secs));
        dirty = true;

        String reason = why == 1 ? "NO ANSWER" : "CALL ENDED";
        if (server == null) return;
        ServerPlayer pa = server.getPlayerList().getPlayer(c.caller);
        ServerPlayer pb = server.getPlayerList().getPlayer(c.callee);
        if (pa != null) {
            callEvent(pa, PhoneNet.EVT_ENDED, b.number, secs, reason);
            sendState(pa);
        }
        if (pb != null) {
            callEvent(pb, PhoneNet.EVT_ENDED, a.number, secs, reason);
            sendState(pb);
        }
    }

    // ================================================================== YenApp

    private static void yenResult(ServerPlayer p, int op, boolean ok, String message, String tag) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(op);
        buf.writeBoolean(ok);
        buf.writeUtf(message, 200);
        buf.writeUtf(tag, 32);
        ServerPlayNetworking.send(p, PhoneNet.S2C_YEN, buf);
    }

    private static boolean validTag(String t) {
        return t.length() >= PhoneNet.TAG_MIN && t.length() <= PhoneNet.TAG_MAX && t.matches("[A-Za-z0-9_.]+");
    }

    private static void yen(ServerPlayer p, int op, String argRaw, int amount) {
        PhoneProfile me = profile(p);
        String arg = argRaw == null ? "" : argRaw.trim();

        if (op == PhoneNet.YEN_REGISTER) {
            if (!me.tag.isEmpty()) {
                yenResult(p, op, false, "You already have a \u00a5tag. It can't be changed.", me.tag);
                return;
            }
            if (!validTag(arg)) {
                yenResult(p, op, false, "Use 1-10 letters, numbers, _ or .", "");
                return;
            }
            String key = arg.toLowerCase(Locale.ROOT);
            if (BY_TAG.containsKey(key)) {
                yenResult(p, op, false, "That \u00a5tag is already taken.", "");
                return;
            }
            me.tag = arg;
            BY_TAG.put(key, p.getUUID());
            saveNow(); // economy data is written immediately
            yenResult(p, op, true, "Your \u00a5tag is " + arg + ".", arg);
            sendState(p);
            return;
        }

        UUID toId = arg.isEmpty() ? null : BY_TAG.get(arg.toLowerCase(Locale.ROOT));
        if (op == PhoneNet.YEN_LOOKUP) {
            if (toId == null || toId.equals(p.getUUID())) {
                yenResult(p, op, false, "", "");
            } else {
                yenResult(p, op, true, "", profile(toId).tag); // canonical capitalisation
            }
            return;
        }

        if (op == PhoneNet.YEN_PAY) {
            if (me.tag.isEmpty()) {
                yenResult(p, op, false, "Create your \u00a5tag first.", "");
                return;
            }
            if (toId == null) {
                yenResult(p, op, false, "That \u00a5tag does not exist.", "");
                return;
            }
            if (toId.equals(p.getUUID())) {
                yenResult(p, op, false, "You can't pay yourself.", "");
                return;
            }
            if (amount < PhoneNet.PAY_MIN) {
                yenResult(p, op, false, "Minimum payment is \u00a510.", "");
                return;
            }
            if (amount > PhoneNet.PAY_MAX) {
                yenResult(p, op, false, "Maximum payment is \u00a5500,000.", "");
                return;
            }
            PhoneProfile to = profile(toId);
            if (me.yen < amount) {
                yenResult(p, op, false, "Not enough yen.", "");
                return;
            }
            if (to.yen > Long.MAX_VALUE - amount) {
                yenResult(p, op, false, "That wallet can't receive more.", "");
                return;
            }
            me.yen -= amount; // both changes happen together on the server thread: nothing can interleave
            to.yen += amount;
            saveNow();
            yenResult(p, op, true, "Paid \u00a5" + String.format("%,d", amount) + " to " + to.tag + ".", to.tag);
            sendState(p);
            ServerPlayer rec = server.getPlayerList().getPlayer(toId);
            if (rec != null) {
                notice(rec, "You received \u00a5" + String.format("%,d", amount) + " from " + me.tag + ".");
                sendState(rec);
            }
        }
    }

    // ================================================================== used by the /phone command

    public static long getYen(UUID id) {
        return profile(id).yen;
    }

    public static void setYen(UUID id, long value) {
        profile(id).yen = Math.max(0, value);
        saveNow();
        refresh(id);
    }

    public static String getNumber(UUID id) {
        return profile(id).number;
    }
}
