package com.niko.ragnarok.entity.others;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class RkSmoothMoveControl extends MoveControl {
    private final float maxTurnRate;  // 通常移動時の補間スピード
    private int recalculateDelay = 0; // 次の計算までの待機カウンター

    public RkSmoothMoveControl(Mob mob, float maxTurnRate) {
        super(mob);
        this.maxTurnRate = maxTurnRate;
    }

    @Override
    public void tick() {
        if (this.operation == MoveControl.Operation.MOVE_TO) {
            this.operation = MoveControl.Operation.WAIT;

            Vec3 currentPos = this.mob.position();
            Vec3 targetPos = new Vec3(this.wantedX, this.wantedY, this.wantedZ);

            double dx = this.wantedX - this.mob.getX();
            double dz = this.wantedZ - this.mob.getZ();
            double dy = this.wantedY - this.mob.getY();
            double sqDistance = dx * dx + dy * dy + dz * dz;

            if (sqDistance < 0.0001) {
                this.mob.setZza(0.0F);
                return;
            }

            // 目標とするY軸回転角度
            float targetYaw = (float)(Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;

            // 1. タイマー更新と再計算フラグの判定
            boolean shouldRecalculate = false;
            if (this.recalculateDelay <= 0) {
                shouldRecalculate = true;
                // 10〜15tickの間でランダムに次回タイマーをリセット（ランダム化で複数Mobの処理負荷分散にも寄与）
                this.recalculateDelay = 10 + this.mob.getRandom().nextInt(6);
            } else {
                this.recalculateDelay--;
            }

            // 2. 現在地から目的地までの間に壁（ブロック）があるかレイキャストで判定
            boolean hasObstacle = hasObstacleInPath(currentPos, targetPos);

            float turnRateToUse = this.maxTurnRate;

            // タイマー満了時（10~15tick周期）、または壁がある場合は「爆速（瞬時）」で旋回
            if (shouldRecalculate || hasObstacle) {
                turnRateToUse = 180.0F; // 即座にターゲット方向へ向き直す
            }

            // 角度の補間処理
            float nextYaw = this.rotlerp(this.mob.getYRot(), targetYaw, turnRateToUse);
            this.mob.setYRot(nextYaw);
            this.mob.yBodyRot = nextYaw; // 体の向きも合わせる

            // 移動速度の設定
            this.mob.setSpeed((float)(this.speedModifier * this.mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)));
        } else {
            this.mob.setZza(0.0F);
            this.recalculateDelay = 0; // 移動停止時はタイマーをリセット
        }
    }

    /**
     * モブの現在地と目的地の間にブロックが存在するか判定するレイキャスト
     */
    private boolean hasObstacleInPath(Vec3 start, Vec3 end) {
        Vec3 eyeStart = start.add(0, this.mob.getEyeHeight() * 0.5, 0);
        Vec3 eyeEnd = end.add(0, this.mob.getEyeHeight() * 0.5, 0);

        HitResult result = this.mob.level().clip(new ClipContext(
                eyeStart,
                eyeEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this.mob
        ));

        return result.getType() == HitResult.Type.BLOCK;
    }
}