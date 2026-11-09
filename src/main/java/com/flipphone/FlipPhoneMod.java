package com.flipphone;

import com.flipphone.server.PhoneCommand;
import com.flipphone.server.PhoneService;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

/** Common (server + client) entrypoint of the Flip Phone mod. */
public class FlipPhoneMod implements ModInitializer {
    public static final String MOD_ID = "flipphone";

    public static Item PHONE;

    /** Set by the client entrypoint; the item calls it on right-click. Does nothing on a dedicated server. */
    public static Runnable CLIENT_OPEN = () -> { };

    @Override
    public void onInitialize() {
        PHONE = Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(MOD_ID, "phone"),
                new PhoneItem(new Item.Properties().stacksTo(1)));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> entries.accept(PHONE));

        PhoneService.init();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> PhoneCommand.register(dispatcher));
    }
}
