package com.niko.ragnarok.entity.ai;

import com.niko.ragnarok.entity.costom.Mini_Groot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

public class MiniGrootMeleeAttackGoal extends MeleeAttackGoal {
    private final Mini_Groot mob;
    private int attackTimer = 0; // アニメーション完遂用タイマー

    public MiniGrootMeleeAttackGoal(Mini_Groot mob, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        super(mob, speedModifier, followingTargetEvenIfNotSeen);
        this.mob = mob;
    }

    @Override
    public void tick() {
        // 1. 移動やターゲット確認（super.tick() を呼ぶと内部で攻撃判定が走り、
        // クールタイムが勝手に加算されることがあるので注意が必要だ）
        super.tick();

        LivingEntity target = this.mob.getTarget();
        if (target == null) return;

        // 2. アニメーション進行中の処理
        if (this.attackTimer > 0) {
            this.attackTimer--;

            // ダメージ発生（10tick目）
            if (this.attackTimer == 4) {
                this.mob.doHurtTarget(target);
            }

            // 【ここが重要】アニメーションが終わった瞬間
            if (this.attackTimer <= 0) {
                this.mob.setAttacking(false);

                // バニラの MeleeAttackGoal クラスにある「次に殴れるまでの時間」を
                // 強制的に 0 に書き換えて、即座に次の canUse / 攻撃判定を許可する
                this.resetAttackCooldown(); // これで ticksUntilNextAttack = 0 になる
            }
        } else {
            // 3. 攻撃待機中の処理（自前で距離をチェック）
            double distanceSq = this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ());

            // 射程内かつ、バニラ側のクールタイムが 0 以下なら次へ
            if (distanceSq <= 12.25D && this.getTicksUntilNextAttack() <= 0) {
                this.mob.setAttacking(true);
                this.attackTimer = 14;
                // 攻撃開始時に一度リセット（これを行わないと super.tick 内で二重にカウントされることがある）
                this.resetAttackCooldown();
            }
        }
    }
    @Override
    protected void resetAttackCooldown() {
        super.resetAttackCooldown();
    }
    @Override
    protected double getAttackReachSqr(LivingEntity pAttackTarget) {
        // 基本の射程に余裕（+1.0程度）を持たせることで、密着状態のミスを防ぐ
        return (double)(this.mob.getBbWidth() * 2.0F * this.mob.getBbWidth() * 2.0F + pAttackTarget.getBbWidth()) + 1.0D;
    }
    private void startAttackAnimation(LivingEntity pEnemy) {
        this.mob.setAttacking(true);
        this.attackTimer = 14;
        this.resetAttackCooldown();
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity pEnemy, double pDistToEnemySqr) {
    }

    @Override
    public void stop() {
        super.stop();
        this.attackTimer = 0;
        this.mob.setAttacking(false);
    }
}