package com.niko.ragnarok.entity.AI;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.Predicate;

/**
 * 直接移動追跡ゴール – ナビゲーションに頼らない直接制御
 * プレイヤー移動に即座に反応
 */
public class RkmoveGoal extends Goal {

    private final Mob mob;
    private final double speed;
    private final Predicate<Mob> canMovePredicate;

    private LivingEntity target;

    public RkmoveGoal(Mob mob, double speed, Predicate<Mob> canMovePredicate) {
        this.mob = mob;
        this.speed = speed;
        this.canMovePredicate = canMovePredicate;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity t = mob.getTarget();
        return t != null && t.isAlive() && canMovePredicate.test(mob);
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity t = mob.getTarget();
        return t != null && t.isAlive() && canMovePredicate.test(mob);
    }

    @Override
    public void start() {
        this.target = mob.getTarget();
        mob.getNavigation().stop();
    }

    @Override
    public void stop() {
        this.target = null;
        mob.getNavigation().stop();
        mob.setZza(0.0F);
        mob.setXxa(0.0F);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity t = mob.getTarget();
        if (t == null || !t.isAlive()) {
            return;
        }
        this.target = t;

        // ターゲットを見つめる
        mob.getLookControl().setLookAt(t, 30F, 30F);
        
        // 直接移動
        moveTowardTarget(t);
    }

    /**
     * ターゲットに向かって直接移動する
     * NavigationSystem を使わずに直接 DeltaMovement を操作
     */
    private void moveTowardTarget(LivingEntity target) {
        Vec3 mobPos = mob.position();
        Vec3 targetPos = target.position();
        Vec3 direction = targetPos.subtract(mobPos);
        
        // XZ平面の距離を計算
        double xzDistSq = direction.x * direction.x + direction.z * direction.z;
        
        // 十分近い場合は移動を止める
        if (xzDistSq < 0.01D) {
            mob.setZza(0.0F);
            mob.setXxa(0.0F);
            return;
        }
        
        // 正規化して方向を取得（Y成分は除外）
        double xzDist = Math.sqrt(xzDistSq);
        double dirX = direction.x / xzDist;
        double dirZ = direction.z / xzDist;
        
        // ターゲット方向に向く（スムーズな回転）
        float targetYaw = (float) (Math.atan2(dirZ, dirX) * (180F / Math.PI)) - 90F;
        float currentYaw = mob.getYRot();
        float yawDiff = net.minecraft.util.Mth.wrapDegrees(targetYaw - currentYaw);
        float maxTurn = 15F; // 1tick あたりの最大回転角
        
        if (yawDiff > maxTurn) yawDiff = maxTurn;
        if (yawDiff < -maxTurn) yawDiff = -maxTurn;
        
        float newYaw = currentYaw + yawDiff;
        mob.setYRot(newYaw);
        mob.yBodyRot = newYaw;
        mob.yHeadRot = newYaw;
        
        // 速度を直接設定（移動速度属性を参照）
        double moveSpeed = speed * mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        
        // 前進方向に移動（現在の向きに基づいて）
        Vec3 lookAngle = mob.getLookAngle();
        double moveX = lookAngle.x * moveSpeed;
        double moveZ = lookAngle.z * moveSpeed;
        
        // 重力は保持
        double moveY = mob.getDeltaMovement().y;
        
        mob.setDeltaMovement(moveX, moveY, moveZ);
        
        // 段差を越えられるようにジャンプ判定
        if (targetPos.y > mobPos.y + 0.1D && 
            targetPos.y <= mobPos.y + mob.getStepHeight() + 0.1D) {
            mob.getJumpControl().jump();
        }
    }
}
