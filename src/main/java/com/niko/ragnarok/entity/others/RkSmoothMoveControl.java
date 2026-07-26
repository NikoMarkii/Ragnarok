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

    // 水平距離がこれ未満のときは方向計算をスキップし、直前の向きを維持する。
    // ターゲットがすぐ近く（または真横〜背後）にいるとき、位置のわずかなブレで
    // atan2の結果が大きく暴れ、後ろを向いてしまう現象を防ぐ。
    private static final double MIN_YAW_UPDATE_DIST_SQ = 0.64D; // 約0.8ブロック

    // 水平距離がこれを超えたときは「瞬時180度旋回」を禁止し、通常のmaxTurnRateで
    // ゆっくり旋回させる。角度計算自体は行うので遠距離でもちゃんとターゲット方向へ
    // 向き直るが、目的地が大きくジャンプした直後などに一瞬で反転する見た目を防ぐ。
    private static final double MAX_INSTANT_TURN_DIST_SQ = 400.0D; // 約20ブロック

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
            // 水平距離が近すぎる場合は、位置のブレでatan2が暴れて反転するのを防ぐため
            // 角度計算そのものをスキップし、現在の向きを維持する。
            // （遠距離側は角度計算自体は通常通り行い、瞬時旋回のみ下で別途制限する）
            double horizontalDistSq = dx * dx + dz * dz;
            float targetYaw;
            if (horizontalDistSq < MIN_YAW_UPDATE_DIST_SQ) {
                targetYaw = this.mob.getYRot();
            } else {
                targetYaw = (float)(Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
            }

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

            // タイマー満了時（10~15tick周期）、または壁がある場合は「爆速（瞬時）」で旋回。
            // ただし遠距離（MAX_INSTANT_TURN_DIST_SQ超）では瞬時旋回を許可せず、
            // 通常のmaxTurnRateでゆっくり向き直す。
            boolean allowInstantTurn = horizontalDistSq <= MAX_INSTANT_TURN_DIST_SQ;
            if ((shouldRecalculate || hasObstacle) && allowInstantTurn) {
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