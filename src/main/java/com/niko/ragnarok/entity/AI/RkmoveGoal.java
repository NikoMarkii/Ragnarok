package com.niko.ragnarok.entity.AI;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.Predicate;

/**
 * 改善版追跡ゴール – ナビゲーション + 高速再計算
 * プレイヤー移動に即座に反応しつつ、障害物対応も可能
 */
public class RkmoveGoal extends Goal {

    private final Mob mob;
    private final double speed;
    private final Predicate<Mob> canMovePredicate;

    private LivingEntity target;
    private int pathRecalcTimer = 0;
    private Vec3 lastTargetPos = Vec3.ZERO;
    private static final double RECALC_DISTANCE_THRESHOLD = 0.5D; // 0.5ブロック移動で再計算
    private static final int MIN_RECALC_INTERVAL = 1; // 最小1tick間隔で再計算可能

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
        if (t == null || !t.isAlive()) {
            return;
        }
        this.target = t;

        // ターゲットを見つめる
        mob.getLookControl().setLookAt(t, 30F, 30F);
        
        // 経路の再計算判定
        updatePath(t);
    }

    /**
     * ナビゲーション経路を積極的に更新
     * プレイヤーの移動に素早く反応
     */
    private void updatePath(LivingEntity target) {
        Vec3 targetPos = target.position();
        
        // ターゲット移動距離を計算
        double distFromLastPos = targetPos.distanceToSqr(this.lastTargetPos);
        
        // 毎tick再計算（または閾値以上の移動で即座に再計算）
        if (distFromLastPos > RECALC_DISTANCE_THRESHOLD * RECALC_DISTANCE_THRESHOLD) {
            // プレイヤーが大きく移動した → 即座に経路再計算
            this.pathRecalcTimer = 0;
            this.lastTargetPos = targetPos;
        }
        
        // カウントダウン
        this.pathRecalcTimer--;
        
        // 経路の再計算実行
        if (this.pathRecalcTimer <= 0) {
            // 毎tickナビゲーション指令を送信（ただし非常に短い間隔で）
            // これにより、ナビゲーションシステムがプレイヤーの最新位置を常に参照する
            this.mob.getNavigation().moveTo(target, this.speed);
            
            // 次の再計算までの短い間隔（1-2tick）
            this.pathRecalcTimer = MIN_RECALC_INTERVAL;
            this.lastTargetPos = targetPos;
        }
    }
}
