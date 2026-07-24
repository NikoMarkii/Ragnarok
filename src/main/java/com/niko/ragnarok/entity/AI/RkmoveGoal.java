package com.niko.ragnarok.entity.AI;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.Predicate;

/**
 * 汎用追跡ゴール – 移動可能条件を外部から指定できる
 */
public class RkmoveGoal extends Goal {

    private final Mob mob;
    private final double speed;
    private final Predicate<Mob> canMovePredicate;

    private LivingEntity target;
    private int pathRecalcTimer;
    private Vec3 lastTargetPos = Vec3.ZERO;

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
        tryRecalcPath(t);
    }

    private void tryRecalcPath(LivingEntity t) {
        this.pathRecalcTimer--;

        // タイマーが0以下になった時のみ、再計算の余地を与える
        if (this.pathRecalcTimer <= 0) {
            boolean navDone = this.mob.getNavigation().isDone();
            boolean targetMoved = t.position().distanceToSqr(this.lastTargetPos) > 1.0D;

            // ナビゲーションが終わっているか、ターゲットが移動していたらパスを更新
            if (navDone || targetMoved) {
                // 再計算のインターバルをランダムに設定（バニラの挙動を参考にした軽量化対策）
                this.pathRecalcTimer = 4 + this.mob.getRandom().nextInt(7);
                this.lastTargetPos = t.position();
                this.mob.getNavigation().moveTo(t, this.speed);
            }
        }
    }
}
