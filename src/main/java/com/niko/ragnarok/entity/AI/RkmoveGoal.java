package com.niko.ragnarok.entity.AI;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.Predicate;

/**
 * 汎用追跡ゴール – 移動可能条件を外部から指定できる
 * 改善: より直線的で自然な追従、ナビゲーションの遅延を軽減
 */
public class RkmoveGoal extends Goal {

    private final Mob mob;
    private final double speed;
    private final Predicate<Mob> canMovePredicate;

    private LivingEntity target;
    private int pathRecalcTimer;
    private Vec3 lastTargetPos = Vec3.ZERO;
    private static final double RECALC_DISTANCE_SQ = 4.0D; // 2ブロック移動で再計算

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
        this.pathRecalcTimer = 0;
        this.lastTargetPos = target != null ? target.position() : Vec3.ZERO;
    }

    @Override
    public void stop() {
        this.target = null;
        mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity t = mob.getTarget();
        if (t == null || !t.isAlive()) return;
        this.target = t;

        mob.getLookControl().setLookAt(t, 30F, 30F);
        
        // ターゲットが非常に近い場合は、ナビゲーションを使わず直接移動
        double distSq = mob.distanceToSqr(t);
        if (distSq < 6.25D) { // 2.5ブロック以内は直接移動
            moveDirectlyToTarget(t);
        } else {
            // それ以外はナビゲーションを使用（パス検索）
            tryRecalcPath(t);
        }
    }

    /**
     * ターゲットに直接移動（ナビゲーション不使用）
     * より滑らかで、横歩きが少ない
     */
    private void moveDirectlyToTarget(LivingEntity target) {
        Vec3 mobPos = mob.position();
        Vec3 targetPos = target.position();
        Vec3 direction = targetPos.subtract(mobPos);
        
        // Y成分を無視（垂直移動はジャンプで処理）
        direction = new Vec3(direction.x, 0, direction.z);
        
        double distSq = direction.lengthSqr();
        if (distSq > 0.01) {
            direction = direction.normalize();
            double moveSpeed = speed * mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            mob.setDeltaMovement(
                    direction.x * moveSpeed,
                    mob.getDeltaMovement().y,
                    direction.z * moveSpeed
            );
            
            // 段差を越える判定
            if (targetPos.y > mobPos.y + mob.getStepHeight()) {
                mob.getJumpControl().jump();
            }
        }
    }

    /**
     * ナビゲーション経路の再計算
     * より頻繁にターゲット位置の変化をチェック
     */
    private void tryRecalcPath(LivingEntity t) {
        this.pathRecalcTimer--;

        // ターゲットが大きく移動したか、パスが完了したら再計算
        if (this.pathRecalcTimer <= 0) {
            boolean navDone = this.mob.getNavigation().isDone();
            boolean targetMoved = t.position().distanceToSqr(this.lastTargetPos) > RECALC_DISTANCE_SQ;

            if (navDone || targetMoved) {
                // 再計算のインターバル（ターゲットが移動していたら短くする）
                this.pathRecalcTimer = targetMoved ? 3 : (5 + this.mob.getRandom().nextInt(5));
                this.lastTargetPos = t.position();
                this.mob.getNavigation().moveTo(t, this.speed);
            }
        }
    }
}
