package com.niko.ragnarok.entity.MoveControl;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;

/**
 * 体（yBodyRot）の回転をパスの経由点ではなく、
 * 「現在のターゲット」または「移動先座標」への直線方向に
 * 一定の最大回転速度でなめらかに追従させる MoveControl。
 *
 * バニラの MoveControl は最大90度/tickで経由点方向へ回転するため、
 * 斜め移動時のパス経由点の切り替わりで体が不自然にブレる。
 * これを抑えるため、
 *  1. 最大回転速度を落とす
 *  2. ターゲットが存在する場合はターゲット本体への直線方向を優先する
 * という2点で対応する。
 */
public class SmoothTargetMoveControl extends MoveControl {

    private final Mob mob;
    private final float maxTurnPerTick;
    private boolean movementDisabled = false;

    public SmoothTargetMoveControl(Mob mob) {
        this(mob, 20.0F); // 1tickあたりの最大回転角。値を小さくするほど滑らかになるが反応が遅くなる
    }
    public void setOperation(Operation op) {
        this.operation = op;
    }
    public void setMovementDisabled(boolean disabled) {
        this.movementDisabled = disabled;
    }

    public SmoothTargetMoveControl(Mob mob, float maxTurnPerTick) {
        super(mob);
        this.mob = mob;
        this.maxTurnPerTick = maxTurnPerTick;
    }

    @Override
    public void tick() {
        if (this.operation != Operation.MOVE_TO) {
            this.mob.setSpeed(0.0F);
            return;
        }

        double dx = this.wantedX - this.mob.getX();
        double dy = this.wantedY - this.mob.getY();
        double dz = this.wantedZ - this.mob.getZ();
        double distSq = dx * dx + dy * dy + dz * dz;

        if (distSq < 2.5000003E-7) {
            this.mob.setSpeed(0.0F);
            if (!this.movementDisabled) {   // 移動無効中は operation を変更しない
                this.operation = Operation.WAIT;
            }
            return;
        }

        // --- 回転処理（常に実行） ---
        double dirX = dx;
        double dirZ = dz;
        LivingEntity target = this.mob.getTarget();
        if (target != null && target.isAlive()) {
            dirX = target.getX() - this.mob.getX();
            dirZ = target.getZ() - this.mob.getZ();
        }
        float targetYaw = (float) (Mth.atan2(dirZ, dirX) * (180.0 / Math.PI)) - 90.0F;
        this.mob.setYRot(rotlerp(this.mob.getYRot(), targetYaw, this.maxTurnPerTick));

        // --- 速度設定（フラグで制御） ---
        if (this.movementDisabled) {
            this.mob.setSpeed(0.0F);
            return;   // ジャンプ処理もスキップ
        }

        this.mob.setSpeed((float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));

        // 以下、ジャンプ処理（変更なし）
        if (dy > (double) this.mob.maxUpStep() && dx * dx + dz * dz < Math.max(1.0, this.mob.getBbWidth())) {
            this.mob.getJumpControl().jump();
            this.operation = Operation.JUMPING;
        }
        if (this.operation == Operation.JUMPING) {
            this.mob.setSpeed((float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
            if (this.mob.onGround()) {
                this.operation = Operation.WAIT;
            }
            return;
        }
        if (this.operation != Operation.MOVE_TO) {
            this.mob.setSpeed(0);
        }
    }
    private static float clampedRotlerp(float current, float target, float maxChange) {
        float diff = Mth.wrapDegrees(target - current);
        diff = Mth.clamp(diff, -maxChange, maxChange);
        return current + diff;
    }
}
