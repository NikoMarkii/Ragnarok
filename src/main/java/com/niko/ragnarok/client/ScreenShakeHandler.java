package com.niko.ragnarok.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ScreenShakeHandler {

    private static float intensity = 0;
    private static int remainingTicks = 0;

    public static void startShake(float intensity, int duration) {
        ScreenShakeHandler.intensity = intensity;
        ScreenShakeHandler.remainingTicks = duration;
    }

    // RenderLevelStageEventやClientTickEventで毎tick呼ぶ
    public static void tick() {
        if (remainingTicks > 0) {
            remainingTicks--;
        }
    }

    public static float getCurrentOffsetX() {
        if (remainingTicks <= 0) return 0;
        return (float)(Math.random() * 2 - 1) * intensity;
    }

    public static float getCurrentOffsetY() {
        if (remainingTicks <= 0) return 0;
        return (float)(Math.random() * 2 - 1) * intensity;
    }
}
