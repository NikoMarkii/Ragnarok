package com.niko.ragnarok.entity.ai;

import com.niko.ragnarok.entity.costom.Scorpion;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;


public class ScorpionAttackGoal extends MeleeAttackGoal {
    private final Scorpion scorpion;
    private final double speedModifier;
    private int attackCooldown = 0;

    public ScorpionAttackGoal(Scorpion scorpion, double speed, boolean useLongMemory) {
        super(scorpion, speed, useLongMemory);
        this.scorpion = scorpion;
        this.speedModifier = speed;
    }

    @Override
    protected double getAttackReachSqr(LivingEntity target) {
        double reach = (this.scorpion.getBbWidth() * 1.5F) * (this.scorpion.getBbWidth() * 1.5F) + target.getBbWidth();
        return reach;
    }

    @Override
    public boolean canContinueToUse() {
        return scorpion.isAttacking() || super.canContinueToUse();
    }

    @Override
    public void tick() {
        LivingEntity target = scorpion.getTarget();
        if (target == null || !target.isAlive()) return;

        double distance = scorpion.distanceToSqr(target.getX(), target.getY(), target.getZ());
        scorpion.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // 追跡は常に通常速度
        scorpion.getNavigation().moveTo(target, speedModifier);

        if (attackCooldown > 0) attackCooldown--;

        // 攻撃開始
        if (distance < getAttackReachSqr(target) && !scorpion.isAttacking() && attackCooldown <= 0) {
            scorpion.setAttacking(true);
            scorpion.attackAnimationTimeout = Scorpion.ATTACK_ANIM_TICKS;
            scorpion.hasDealtStingDamage = false;
            if (scorpion.attackAnimationState.isStarted()) scorpion.attackAnimationState.stop();
            scorpion.attackAnimationState.start(scorpion.tickCount);
            attackCooldown = Scorpion.ATTACK_ANIM_TICKS + 4;
        }

        // 攻撃判定
        if (scorpion.isAttacking()) {
            scorpion.attackAnimationTimeout--;

            if (scorpion.attackAnimationTimeout == Scorpion.ATTACK_ANIM_TICKS - Scorpion.STING_TICK
                    && !scorpion.hasDealtStingDamage) {
                scorpion.performStingAttack(target);
                scorpion.hasDealtStingDamage = true;
            }

            // アニメーションは最後まで再生する
            if (scorpion.attackAnimationTimeout <= 0) {
                scorpion.setAttacking(false);
                scorpion.hasDealtStingDamage = false;
                scorpion.attackAnimationState.stop();
            }
        }
    }
}
















