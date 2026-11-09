package com.flipphone.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/** Loads and saves {@link PhoneData} as JSON: {@code <world>/flipphone/phones.json}. */
public final class PhoneStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private PhoneStore() {}

    private static Path file(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("flipphone").resolve("phones.json");
    }

    public static PhoneData load(MinecraftServer server) {
        Path f = file(server);
        try {
            if (Files.isRegularFile(f)) {
                PhoneData d = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), PhoneData.class);
                if (d != null) return d.sanitize();
            }
        } catch (Exception e) {
            System.err.println("[FlipPhone] Could not read " + f + ": " + e);
        }
        return new PhoneData();
    }

    public static void save(MinecraftServer server, PhoneData data) {
        Path f = file(server);
        try {
            Files.createDirectories(f.getParent());
            Files.writeString(f, GSON.toJson(data), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[FlipPhone] Could not save " + f + ": " + e);
        }
    }
}
