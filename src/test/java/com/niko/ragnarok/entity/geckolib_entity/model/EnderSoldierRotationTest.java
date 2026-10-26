package com.niko.ragnarok.entity.geckolib_entity.model;

import net.minecraft.util.Mth;

public final class EnderSoldierRotationTest {
    public static void main(String[] args) {
        for (float turn : new float[]{360, -360}) {
            for (float blend : new float[]{0, 0.25F, 0.5F, 0.75F, 1}) {
                float angle = EnderSoldierModel.blendRotation(blend, turn * Mth.DEG_TO_RAD, 0);
                if (Math.abs(Mth.wrapDegrees(angle * Mth.RAD_TO_DEG)) > 0.001F) {
                    throw new AssertionError("Finisher unwound during transition: " + angle);
                }
            }
        }
        float halfway = EnderSoldierModel.blendRotation(0.5F, 170 * Mth.DEG_TO_RAD, -170 * Mth.DEG_TO_RAD);
        if (Math.abs(Math.abs(halfway * Mth.RAD_TO_DEG) - 180) > 0.001F) {
            throw new AssertionError("Rotation transition did not take the shortest path");
        }
    }
}
