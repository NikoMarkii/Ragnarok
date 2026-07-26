package com.niko.ragnarok.entity.others;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * 戦闘系の向き制御ユーティリティ - Ragnarok Mod 共通AI
 *
 * 攻撃中はMoveControl/BodyRotationControlに向きを任せず、
 * このクラスで直接ターゲット方向へ体・頭を向けさせる。
 * どのモブのAttackGoalからでも使い回せる想定。
 */
public final class RkCombatUtil {

    private RkCombatUtil() {}

    /** デフォルトの旋回速度（1tickあたりの最大回転角） */
    public static final float DEFAULT_ATTACK_TURN_RATE = 25.0F;

    /**
     * ターゲット方向へ体・頭を向ける（旋回速度を指定）。
     * 攻撃モーション中、毎tick呼び出す想定。
     *
     * @param mob            向きを変えさせたいモブ
     * @param target         注視・攻撃対象
     * @param maxTurnPerTick 1tickあたりの最大回転角（度）。大きいほど瞬時に近くなる
     */
    public static void faceTarget(Mob mob, LivingEntity target, float maxTurnPerTick) {
        double dx = target.getX() - mob.getX();
        double dz = target.getZ() - mob.getZ();

        // 真上/真下などXZ距離がほぼ0の場合は向きを変えない（atan2の暴れ防止）
        if (dx * dx + dz * dz < 1.0E-4) return;

        float targetYaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        float newYaw = rotlerp(mob.getYRot(), targetYaw, maxTurnPerTick);

        mob.setYRot(newYaw);
        mob.yBodyRot = newYaw;
        mob.yHeadRot = newYaw;
    }

    /** デフォルトの旋回速度（{@link #DEFAULT_ATTACK_TURN_RATE}）でターゲットへ向ける */
    public static void faceTarget(Mob mob, LivingEntity target) {
        faceTarget(mob, target, DEFAULT_ATTACK_TURN_RATE);
    }

    private static float rotlerp(float current, float target, float maxChange) {
        float diff = Mth.wrapDegrees(target - current);
        if (diff > maxChange) diff = maxChange;
        if (diff < -maxChange) diff = -maxChange;
        return current + diff;
    }
}
