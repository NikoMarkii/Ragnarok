package com.niko.ragnarok.entity.others;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public final class EnderSoldierBeam {
    public static final int START_TICK = 20;
    public static final int AIM_LOCK_TICK = START_TICK - 8;
    public static final int TRACKING_START_TICK = START_TICK + 4;
    public static final float TRACKING_DEGREES_PER_TICK = 1.5F;
    public static final int END_TICK = 50;
    public static final int DURATION = 70;
    public static final int COOLDOWN = 160;
    public static final int DAMAGE_INTERVAL = 5;
    public static final int DEBUFF_DURATION = 100;
    public static final double RANGE = 32.0D;
    public static final double RADIUS = 0.38D;
    private static final Vec3[] MOUTH_OFFSETS = bakeMouthOffsets();

    private EnderSoldierBeam() {
    }

    public static Vec3 mouthPosition(Vec3 position, float bodyYaw, int attackTick) {
        int sample = Math.max(0, Math.min(MOUTH_OFFSETS.length - 1, attackTick - START_TICK));
        return position.add(MOUTH_OFFSETS[sample].yRot((float) Math.toRadians(180.0F - bodyYaw)));
    }

    private static Vec3[] bakeMouthOffsets() {
        JsonObject geometry = readResource("geo/ender_soldier2.geo.json")
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        JsonObject animation = readResource("animations/ender_soldier2.animation.json")
                .getAsJsonObject("animations").getAsJsonObject("attack4").getAsJsonObject("bones");
        Map<String, JsonObject> bones = new HashMap<>();
        for (JsonElement element : geometry.getAsJsonArray("bones")) {
            JsonObject bone = element.getAsJsonObject();
            bones.put(bone.get("name").getAsString(), bone);
        }
        List<JsonObject> chain = new ArrayList<>();
        String name = "mouth";
        while (name != null) {
            JsonObject bone = bones.get(name);
            if (bone == null || chain.size() >= bones.size()) {
                throw new IllegalStateException("Invalid ender soldier mouth bone hierarchy: " + name);
            }
            chain.add(bone);
            name = bone.has("parent") ? bone.get("parent").getAsString() : null;
        }
        Collections.reverse(chain);
        Vec3 mouthPivot = vector(bones.get("mouth").get("pivot"), Vec3.ZERO).multiply(-1, 1, 1).scale(1.0D / 16.0D);
        Vec3[] offsets = new Vec3[END_TICK - START_TICK];
        for (int sample = 0; sample < offsets.length; sample++) {
            double seconds = (START_TICK + sample) / 20.0D;
            Matrix4f transform = new Matrix4f().translate(0.0F, 0.01F, 0.0F);
            for (JsonObject bone : chain) {
                JsonObject channels = animation.getAsJsonObject(bone.get("name").getAsString());
                Vec3 pivot = vector(bone.get("pivot"), Vec3.ZERO).multiply(-1, 1, 1).scale(1.0D / 16.0D);
                Vec3 rotation = vector(bone.get("rotation"), Vec3.ZERO)
                        .add(sample(channels, "rotation", seconds, Vec3.ZERO));
                Vec3 offset = sample(channels, "position", seconds, Vec3.ZERO).multiply(-1, 1, 1).scale(1.0D / 16.0D);
                Vec3 scale = sample(channels, "scale", seconds, new Vec3(1, 1, 1));
                transform.translate((float) offset.x, (float) offset.y, (float) offset.z)
                        .translate((float) pivot.x, (float) pivot.y, (float) pivot.z)
                        .rotateZ((float) Math.toRadians(rotation.z))
                        .rotateY((float) Math.toRadians(-rotation.y))
                        .rotateX((float) Math.toRadians(-rotation.x))
                        .scale((float) scale.x, (float) scale.y, (float) scale.z)
                        .translate((float) -pivot.x, (float) -pivot.y, (float) -pivot.z);
            }
            Vector3f transformed = transform.transformPosition(mouthPivot.toVector3f());
            offsets[sample] = new Vec3(transformed.x, transformed.y, transformed.z);
        }
        return offsets;
    }

    private static Vec3 sample(JsonObject channels, String channel, double time, Vec3 fallback) {
        if (channels == null || !channels.has(channel)) {
            return fallback;
        }
        JsonElement values = channels.get(channel);
        if (!values.isJsonObject() || values.getAsJsonObject().has("vector")) {
            return vector(values, fallback);
        }
        NavigableMap<Double, JsonElement> frames = new TreeMap<>();
        values.getAsJsonObject().entrySet().forEach(entry -> frames.put(Double.parseDouble(entry.getKey()), entry.getValue()));
        if (frames.isEmpty()) {
            return fallback;
        }
        var lower = frames.floorEntry(time);
        var upper = frames.higherEntry(time);
        if (lower == null) {
            return vector(frames.firstEntry().getValue(), fallback);
        }
        Vec3 start = vector(lower.getValue(), fallback);
        if (upper == null) {
            return start;
        }
        Vec3 end = vector(upper.getValue(), fallback);
        double progress = (time - lower.getKey()) / (upper.getKey() - lower.getKey());
        JsonElement frame = upper.getValue();
        boolean smooth = frame.isJsonObject() && frame.getAsJsonObject().has("lerp_mode")
                && frame.getAsJsonObject().get("lerp_mode").getAsString().equals("catmullrom");
        if (!smooth) {
            return start.lerp(end, progress);
        }
        var previous = frames.lowerEntry(lower.getKey());
        var next = frames.higherEntry(upper.getKey());
        Vec3 before = previous == null ? start : vector(previous.getValue(), fallback);
        Vec3 after = next == null ? end : vector(next.getValue(), fallback);
        return start.scale(2).add(end.subtract(before).scale(progress))
                .add(before.scale(2).subtract(start.scale(5)).add(end.scale(4)).subtract(after).scale(progress * progress))
                .add(start.scale(3).subtract(before).subtract(end.scale(3)).add(after).scale(progress * progress * progress))
                .scale(0.5D);
    }

    private static Vec3 vector(JsonElement value, Vec3 fallback) {
        if (value == null) {
            return fallback;
        }
        if (value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            return vector(object.has("post") ? object.get("post") : object.get("vector"), fallback);
        }
        if (value.isJsonPrimitive()) {
            double scalar = value.getAsDouble();
            return new Vec3(scalar, scalar, scalar);
        }
        var components = value.getAsJsonArray();
        return new Vec3(components.get(0).getAsDouble(), components.get(1).getAsDouble(), components.get(2).getAsDouble());
    }

    private static JsonObject readResource(String path) {
        try (InputStream stream = EnderSoldierBeam.class.getResourceAsStream("/assets/ragnarok/" + path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing ender soldier resource: " + path);
            }
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read ender soldier resource: " + path, exception);
        }
    }
}
