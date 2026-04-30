package com.niko.ragnarok.entity.ai;

import com.niko.ragnarok.entity.costom.TLex;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

public class TlexAttackGoal extends MeleeAttackGoal {
    private final TLex tlex;
    private final double speedModifier; // ← ここで自前のspeedを保持する
    private int attackCooldown = 0;

    private static final int ATTACK_ANIM_TICKS = 31;
    private static final int BITE_TICK = 17;

    public boolean hasStung = false;

    public TlexAttackGoal(TLex mob, double speed, boolean useLongMemory) {
        super(mob, speed, useLongMemory);
        this.tlex = mob;
        this.speedModifier = speed; // ← コンストラクタで代入
    }

    @Override
    protected double getAttackReachSqr(LivingEntity target) {
        double reach = (this.mob.getBbWidth() * 2F) * (this.mob.getBbWidth() * 2F)
                + target.getBbWidth();
        return reach;
    }
    @Override
    public boolean canContinueToUse() {
        return tlex.isAttacking() || super.canContinueToUse();
    }

    @Override
    public void tick() {
        LivingEntity target = this.tlex.getTarget();
        if (target == null) return;

        double distanceSqr = this.tlex.distanceToSqr(target.getX(), target.getY(), target.getZ());
        double reachSqr = getAttackReachSqr(target);
        double yDiff = target.getY() - this.tlex.getY();
        boolean isTooHigh = yDiff > 1.0;

        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // --- 攻撃開始判定 ---
        if (distanceSqr <= reachSqr && !isTooHigh && attackCooldown <= 0 && !tlex.isAttacking() && !tlex.isRoaring()) {
            tlex.setAttacking(true);
            tlex.attackAnimationTimeout = 31;
            tlex.hasDealtBiteDamage = false;
            attackCooldown = 40;
            // ★ ここにあった stop() を削除
        }

        // --- 移動制御（止まらない設定） ---
        // 咆哮中でない限り、常に移動を試みる
        if (!tlex.isRoaring()) {
            // 攻撃中であっても、ターゲットとの距離がある程度あれば追い続ける
            // reachSqr * 0.3 など、より小さい値にすることで、重なる直前まで歩み寄るようになる
            if (distanceSqr > reachSqr * 0.3 || isTooHigh) {
                this.mob.getNavigation().moveTo(target, this.speedModifier);
            }

            // 詰まり防止のジャンプ
            if (this.tlex.horizontalCollision && this.tlex.onGround()) {
                this.tlex.getJumpControl().jump();
            }
        } else {
            // 咆哮中のみ威厳を保つために停止
            this.mob.getNavigation().stop();
        }

        // --- ダメージ判定 ---
        if (tlex.isAttacking() && tlex.attackAnimationTimeout == 14 && !tlex.hasDealtBiteDamage) {
            tlex.performBiteAttack(target);
            tlex.hasDealtBiteDamage = true;
        }

        if (attackCooldown > 0) attackCooldown--;
    }
}







