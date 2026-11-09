package com.flipphone.client;

import com.flipphone.FlipPhoneMod;
import com.flipphone.PhoneConfig;
import com.flipphone.client.ui.PhoneScreen;
import com.flipphone.net.PhoneNet;
import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Client entrypoint: key binding, packet receivers, ringing and notifications. */
public class FlipPhoneClient implements ClientModInitializer {
    /** Opens / closes the phone. Rebindable under Controls > Flip Phone (default: Left Alt, like the video). */
    public static KeyMapping TOGGLE;

    private boolean handshakeDone;
    private int ticks;

    @Override
    public void onInitializeClient() {
        TOGGLE = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.flipphone.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, "key.categories.flipphone"));
        FlipPhoneMod.CLIENT_OPEN = FlipPhoneClient::openPhone; // right-clicking the item calls this

        // ---------------------------------------------------------------- packets from the server
        ClientPlayNetworking.registerGlobalReceiver(PhoneNet.S2C_STATE, (client, handler, buf, sender) -> {
            PhoneNet.StateDto s = PhoneNet.StateDto.read(buf);
            client.execute(() -> PhoneClientState.applyState(s));
        });
        ClientPlayNetworking.registerGlobalReceiver(PhoneNet.S2C_HISTORY, (client, handler, buf, sender) -> {
            String number = buf.readUtf(32);
            int n = Math.min(buf.readVarInt(), 300);
            List<PhoneNet.MsgDto> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) list.add(PhoneNet.MsgDto.read(buf));
            client.execute(() -> PhoneClientState.applyHistory(number, list));
        });
        ClientPlayNetworking.registerGlobalReceiver(PhoneNet.S2C_MESSAGE, (client, handler, buf, sender) -> {
            String number = buf.readUtf(32);
            PhoneNet.MsgDto m = PhoneNet.MsgDto.read(buf);
            client.execute(() -> {
                PhoneClientState.applyMessage(number, m);
                if (!m.mine() && !(client.screen instanceof PhoneScreen) && client.player != null) {
                    client.player.displayClientMessage(Component.literal("\u260e New message from " + PhoneClientState.nameOf(number)), true);
                    client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.4f));
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(PhoneNet.S2C_CALL, (client, handler, buf, sender) -> {
            int evt = buf.readVarInt();
            String number = buf.readUtf(32);
            int secs = buf.readVarInt();
            String reason = buf.readUtf(64);
            client.execute(() -> PhoneClientState.applyCall(evt, number, secs, reason));
        });
        ClientPlayNetworking.registerGlobalReceiver(PhoneNet.S2C_YEN, (client, handler, buf, sender) -> {
            int op = buf.readVarInt();
            boolean ok = buf.readBoolean();
            String message = buf.readUtf(200);
            String tag = buf.readUtf(32);
            client.execute(() -> PhoneClientState.applyYen(op, ok, message, tag));
        });
        ClientPlayNetworking.registerGlobalReceiver(PhoneNet.S2C_SOCIAL, (client, handler, buf, sender) -> {
            List<String> list = PhoneNet.readStrings(buf);
            client.execute(() -> PhoneClientState.applySocial(list));
        });
        ClientPlayNetworking.registerGlobalReceiver(PhoneNet.S2C_NOTICE, (client, handler, buf, sender) -> {
            String text = buf.readUtf(200);
            client.execute(() -> {
                PhoneClientState.applyNotice(text);
                if (!(client.screen instanceof PhoneScreen) && client.player != null) {
                    client.player.displayClientMessage(Component.literal("\u260e " + text), true);
                }
            });
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
            PhoneClientState.reset();
            handshakeDone = false;
        }));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);

        // "Incoming call" banner while the phone is closed
        HudRenderCallback.EVENT.register((g, tickDelta) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen instanceof PhoneScreen || mc.options.hideGui) return;
            int phase = PhoneClientState.callPhase;
            if (phase != PhoneNet.EVT_INCOMING && phase != PhoneNet.EVT_RINGING_OUT) return;
            String who = PhoneClientState.nameOf(PhoneClientState.callNumber);
            String text = phase == PhoneNet.EVT_INCOMING
                    ? "\u260e Incoming call: " + who + "  -  press [" + TOGGLE.getTranslatedKeyMessage().getString() + "] to answer"
                    : "\u260e Calling " + who + "...";
            int w = mc.font.width(text) + 16;
            int x = (mc.getWindow().getGuiScaledWidth() - w) / 2;
            int y = 30;
            g.fill(x, y, x + w, y + 16, 0xCC191B18);
            g.fill(x, y + 15, x + w, y + 16, 0xFF06C755);
            g.drawString(mc.font, text, x + 8, y + 4, 0xFFE8EBE0, false);
        });
    }

    private static boolean holdingPhone(Minecraft mc) {
        return mc.player != null && (mc.player.getItemInHand(InteractionHand.MAIN_HAND).getItem() == FlipPhoneMod.PHONE
                || mc.player.getItemInHand(InteractionHand.OFF_HAND).getItem() == FlipPhoneMod.PHONE);
    }

    private static void openPhone() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !(mc.screen instanceof PhoneScreen)) mc.setScreen(new PhoneScreen());
    }

    private void onTick(Minecraft mc) {
        ticks++;
        while (TOGGLE.consumeClick()) {
            if (mc.player != null && mc.screen == null && (!PhoneConfig.KEYBIND_NEEDS_ITEM || holdingPhone(mc))) openPhone();
        }
        if (mc.player == null) return;

        // once per session, shortly after joining: fetch the phone state
        if (!handshakeDone && mc.player.tickCount > 20) {
            handshakeDone = true;
            PhoneClientNet.requestState();
        }

        // ring while a call is ringing (the phone screen may be closed)
        int phase = PhoneClientState.callPhase;
        if ((phase == PhoneNet.EVT_INCOMING || phase == PhoneNet.EVT_RINGING_OUT) && ticks % 30 == 0) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BELL_BLOCK, phase == PhoneNet.EVT_INCOMING ? 1.6f : 1.0f));
        }
    }
}
