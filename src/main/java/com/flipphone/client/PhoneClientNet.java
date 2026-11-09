package com.flipphone.client;

import com.flipphone.net.PhoneNet;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;

/** Everything the phone sends to the server. */
public final class PhoneClientNet {
    private PhoneClientNet() {}

    public static void requestState() {
        ClientPlayNetworking.send(PhoneNet.C2S_REQUEST, PacketByteBufs.create());
    }

    public static void openChat(String number) {
        FriendlyByteBuf b = PacketByteBufs.create();
        b.writeUtf(number, 32);
        ClientPlayNetworking.send(PhoneNet.C2S_CHAT_OPEN, b);
    }

    public static void sendMessage(String number, String text) {
        FriendlyByteBuf b = PacketByteBufs.create();
        b.writeUtf(number, 32);
        b.writeUtf(text, 512);
        ClientPlayNetworking.send(PhoneNet.C2S_SEND, b);
    }

    public static void contact(int op, String number, String nickname, boolean viaSocial) {
        FriendlyByteBuf b = PacketByteBufs.create();
        b.writeVarInt(op);
        b.writeUtf(number, 32);
        b.writeUtf(nickname, 64);
        b.writeBoolean(viaSocial);
        ClientPlayNetworking.send(PhoneNet.C2S_CONTACT, b);
    }

    public static void dial(String number) {
        FriendlyByteBuf b = PacketByteBufs.create();
        b.writeUtf(number, 32);
        ClientPlayNetworking.send(PhoneNet.C2S_DIAL, b);
    }

    public static void callAction(int action) {
        FriendlyByteBuf b = PacketByteBufs.create();
        b.writeVarInt(action);
        ClientPlayNetworking.send(PhoneNet.C2S_CALL, b);
    }

    public static void yen(int op, String arg, int amount) {
        FriendlyByteBuf b = PacketByteBufs.create();
        b.writeVarInt(op);
        b.writeUtf(arg, 64);
        b.writeVarInt(amount);
        ClientPlayNetworking.send(PhoneNet.C2S_YEN, b);
    }

    /** SocialLink heartbeat: true = "I have SocialLink open, scan for me", false = closed. */
    public static void social(boolean open) {
        FriendlyByteBuf b = PacketByteBufs.create();
        b.writeBoolean(open);
        ClientPlayNetworking.send(PhoneNet.C2S_SOCIAL, b);
    }
}
