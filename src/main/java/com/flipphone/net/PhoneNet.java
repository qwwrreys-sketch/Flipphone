package com.flipphone.net;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Channel ids and (de)serialisation of every packet of the phone. Both sides of the mod share this class. */
public final class PhoneNet {
    private PhoneNet() {}

    private static ResourceLocation id(String path) {
        return new ResourceLocation("flipphone", path);
    }

    // ---------------------------------------------------------------- client -> server
    public static final ResourceLocation C2S_REQUEST = id("request");      // "send me my phone state"
    public static final ResourceLocation C2S_CHAT_OPEN = id("chat_open");  // "send me the history with <number>"
    public static final ResourceLocation C2S_SEND = id("send");            // send a message
    public static final ResourceLocation C2S_CONTACT = id("contact");      // add / rename / delete contact
    public static final ResourceLocation C2S_DIAL = id("dial");            // start a call
    public static final ResourceLocation C2S_CALL = id("call");            // answer / hang up / deny
    public static final ResourceLocation C2S_YEN = id("yen");              // register tag / lookup / pay
    public static final ResourceLocation C2S_SOCIAL = id("social");        // SocialLink heartbeat + scan

    // ---------------------------------------------------------------- server -> client
    public static final ResourceLocation S2C_STATE = id("state");
    public static final ResourceLocation S2C_HISTORY = id("history");
    public static final ResourceLocation S2C_MESSAGE = id("message");
    public static final ResourceLocation S2C_CALL = id("call_event");
    public static final ResourceLocation S2C_YEN = id("yen_result");
    public static final ResourceLocation S2C_SOCIAL = id("social_result");
    public static final ResourceLocation S2C_NOTICE = id("notice");

    // ---------------------------------------------------------------- constants shared by both sides
    public static final int CONTACT_ADD = 0;
    public static final int CONTACT_RENAME = 1;
    public static final int CONTACT_DELETE = 2;

    public static final int CALL_ANSWER = 0;
    public static final int CALL_HANGUP = 1;

    public static final int EVT_RINGING_OUT = 0;
    public static final int EVT_INCOMING = 1;
    public static final int EVT_ACTIVE = 2;
    public static final int EVT_ENDED = 3;

    public static final int YEN_REGISTER = 0;
    public static final int YEN_LOOKUP = 1;
    public static final int YEN_PAY = 2;

    public static final int TAG_MIN = 1;
    public static final int TAG_MAX = 10;
    public static final int PAY_MIN = 10;
    public static final int PAY_MAX = 500_000;
    public static final int NICK_MAX = 16;
    public static final int MSG_MAX = 200;
    public static final double SOCIAL_RANGE = 5.0;

    // ---------------------------------------------------------------- data carried by the packets
    public record MsgDto(boolean mine, String text, long time) {
        public void write(FriendlyByteBuf b) {
            b.writeBoolean(mine);
            b.writeUtf(text, 512);
            b.writeLong(time);
        }

        public static MsgDto read(FriendlyByteBuf b) {
            return new MsgDto(b.readBoolean(), b.readUtf(512), b.readLong());
        }
    }

    public record ContactDto(String number, String name) {}

    public record ChatDto(String number, String lastText, boolean lastMine, long lastTime) {}

    public record CallDto(boolean outgoing, String number, String outcome, long time, int seconds) {}

    /** The whole phone state of one player, sent after every change. */
    public record StateDto(String number, long yen, String tag, int msgsSent, int msgsRecv, int calls, long callSeconds,
                           List<ContactDto> contacts, List<ChatDto> chats, List<CallDto> recent) {

        public void write(FriendlyByteBuf b) {
            b.writeUtf(number, 32);
            b.writeLong(yen);
            b.writeUtf(tag, 32);
            b.writeVarInt(msgsSent);
            b.writeVarInt(msgsRecv);
            b.writeVarInt(calls);
            b.writeLong(callSeconds);
            b.writeVarInt(contacts.size());
            for (ContactDto c : contacts) {
                b.writeUtf(c.number(), 32);
                b.writeUtf(c.name(), 64);
            }
            b.writeVarInt(chats.size());
            for (ChatDto c : chats) {
                b.writeUtf(c.number(), 32);
                b.writeUtf(c.lastText(), 512);
                b.writeBoolean(c.lastMine());
                b.writeLong(c.lastTime());
            }
            b.writeVarInt(recent.size());
            for (CallDto c : recent) {
                b.writeBoolean(c.outgoing());
                b.writeUtf(c.number(), 32);
                b.writeUtf(c.outcome(), 32);
                b.writeLong(c.time());
                b.writeVarInt(c.seconds());
            }
        }

        public static StateDto read(FriendlyByteBuf b) {
            String number = b.readUtf(32);
            long yen = b.readLong();
            String tag = b.readUtf(32);
            int sent = b.readVarInt();
            int recv = b.readVarInt();
            int calls = b.readVarInt();
            long secs = b.readLong();
            int n = Math.min(b.readVarInt(), 1000);
            List<ContactDto> contacts = new ArrayList<>(n);
            for (int i = 0; i < n; i++) contacts.add(new ContactDto(b.readUtf(32), b.readUtf(64)));
            n = Math.min(b.readVarInt(), 1000);
            List<ChatDto> chats = new ArrayList<>(n);
            for (int i = 0; i < n; i++) chats.add(new ChatDto(b.readUtf(32), b.readUtf(512), b.readBoolean(), b.readLong()));
            n = Math.min(b.readVarInt(), 1000);
            List<CallDto> recent = new ArrayList<>(n);
            for (int i = 0; i < n; i++) recent.add(new CallDto(b.readBoolean(), b.readUtf(32), b.readUtf(32), b.readLong(), b.readVarInt()));
            return new StateDto(number, yen, tag, sent, recv, calls, secs, contacts, chats, recent);
        }
    }

    public static void writeStrings(FriendlyByteBuf b, List<String> list) {
        b.writeVarInt(list.size());
        for (String s : list) b.writeUtf(s, 64);
    }

    public static List<String> readStrings(FriendlyByteBuf b) {
        int n = Math.min(b.readVarInt(), 256);
        List<String> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(b.readUtf(64));
        return out;
    }
}
