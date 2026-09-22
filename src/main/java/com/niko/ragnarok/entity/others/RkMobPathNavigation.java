package com.niko.ragnarok.entity.others;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public class RkMobPathNavigation extends GroundPathNavigation {

    // 最後にターゲットがいた位置を記録
    private Vec3 lastKnownTargetPos = Vec3.ZERO;
    private int recalculateCooldown = 0;

    private static final double TARGET_MOVE_THRESHOLD_SQR = 4.25D;

    public RkMobPathNavigation(Mob mob, Level level) {
        super(mob, level);
        if (this.nodeEvaluator != null) {
            this.nodeEvaluator.setCanPassDoors(true);
            this.nodeEvaluator.setCanOpenDoors(false);
            this.nodeEvaluator.setCanFloat(true);
        }
    }

    /**
     * 経路計算エントリーポイント
     */
    @Override
    public Path createPath(BlockPos pos, int accuracy) {
        Vec3 targetVec = Vec3.atBottomCenterOf(pos);

        if (this.lastKnownTargetPos != Vec3.ZERO) {
            double distSqr = this.lastKnownTargetPos.distanceToSqr(targetVec);

            // ★ 1. ターゲットの移動がわずか（横移動など）の場合、無駄な経路更新を行わず既存経路をそのまま維持する
            if (distSqr < TARGET_MOVE_THRESHOLD_SQR && this.path != null && !this.path.isDone()) {
                return this.path;
            }

            // ★ 2. ターゲットが遠くへ跳んだ場合の急反転・暴走ディレイ制御
            if (distSqr > 400.0D && this.recalculateCooldown > 0) {
                this.recalculateCooldown--;
                return this.path;
            }
        }

        this.lastKnownTargetPos = targetVec;
        this.recalculateCooldown = 10;

        return super.createPath(pos, accuracy);
    }

    /**
     * 毎tickのナビゲーション更新処理
     */
    @Override
    public void tick() {
        super.tick();

        if (this.isDone()) {
            return;
        }

        // --- 1. 大型モブ特有の「角引っ掛かり」を検知して強行突破・自動補正 ---
        if (this.mob.horizontalCollision) {
            Vec3 moveVec = this.getTempPathPosition();
            if (moveVec != null) {
                if (this.mob.onGround()) {
                    this.mob.getJumpControl().jump();
                }
            }
        }

        // --- 2. 直進性重視：障害物が無ければノードのカクカク（横ブレ）を無視して直線追従 ---
        if (this.path != null && !this.path.isDone() && this.mob.getTarget() != null) {
            Vec3 targetPos = this.mob.getTarget().position();

            // ターゲットとの距離が近く、途中に壁（障害物）が無い場合は直線的に追従
            if (this.mob.distanceToSqr(this.mob.getTarget()) < 256.0D && this.mob.hasLineOfSight(this.mob.getTarget())) {
                // 進行方向をターゲットへ直接固定し、細かな経路ノードへの横揺れを防ぐ
                this.mob.getLookControl().setLookAt(targetPos.x, targetPos.y + this.mob.getTarget().getEyeHeight(), targetPos.z, 10.0F, 10.0F);
            }
        }

        // --- 3. ターゲットが遠距離へ離れたときの向き固定制御 ---
        Path currentPath = this.getPath();
        if (currentPath != null && !currentPath.isDone()) {
            Vec3 nextPos = currentPath.getNextEntityPos(this.mob);
            double distToNext = this.mob.position().distanceToSqr(nextPos);

            if (distToNext > 16.0D) {
                this.mob.setSpeed((float) (this.mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) * 0.9D));
            }
        }
    }

    /**
     * 進行方向にある「一時的な目指すべき位置」を取得
     */
    private Vec3 getTempPathPosition() {
        if (this.path != null && !this.path.isDone()) {
            return this.path.getNextEntityPos(this.mob);
        }
        return null;
    }
}