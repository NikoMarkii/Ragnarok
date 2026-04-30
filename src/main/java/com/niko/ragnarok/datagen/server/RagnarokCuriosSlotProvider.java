package com.niko.ragnarok.datagen.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.CachedOutput;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class RagnarokCuriosSlotProvider implements DataProvider {

    private final PackOutput output;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<String, Integer> SLOTS = new HashMap<>();
    static {
        SLOTS.put("necklace", 1);
        SLOTS.put("head", 1);
        SLOTS.put("hands", 1);
        SLOTS.put("body", 1);
        SLOTS.put("belt", 1);
        SLOTS.put("ring", 2);
        SLOTS.put("back", 1);
        SLOTS.put("charm", 1);
        SLOTS.put("bracelet", 1);
    }

    public RagnarokCuriosSlotProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.runAsync(() -> {
            try {
                Path slotsDir = output.getOutputFolder().resolve("data/ragnarok/curios/slots");
                Files.createDirectories(slotsDir);

                for (Map.Entry<String, Integer> entry : SLOTS.entrySet()) {
                    String slot = entry.getKey();
                    int size = entry.getValue();

                    JsonObject json = new JsonObject();
                    json.addProperty("size", size);

                    Path out = slotsDir.resolve(slot + ".json");
                    System.out.println("Writing Curios slot JSON to: " + out.toAbsolutePath());
                    Files.writeString(out, GSON.toJson(json));
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to write Curios slot JSON", e);
            }
        });
    }

    @Override
    public String getName() {
        return "Eclipse Awakened Curios Slots";
    }
}
