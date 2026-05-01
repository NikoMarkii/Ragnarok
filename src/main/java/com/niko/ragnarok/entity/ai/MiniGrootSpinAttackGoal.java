package com.niko.ragnarok.entity.ai;

import com.niko.ragnarok.entity.costom.Mini_Groot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class MiniGrootSpinAttackGoal extends Goal {
    private final Mini_Groot mob;
    private LivingEntity target;
    private int attackTimer = 0;
    private int attackStep = 0;
    private int globalCooldown = 0; // 次に回転できるまでの時間

    public MiniGrootSpinAttackGoal(Mini_Groot mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // クールタイムを減らす
        if (this.globalCooldown > 0) {
            this.globalCooldown--;
            return false;
        }

        this.target = this.mob.getTarget();
        if (this.target == null || !this.target.isAlive()) return false;

        double distanceSq = this.mob.distanceToSqr(this.target);

        // 【重要】通常攻撃の射程(9.0D)より外側、かつ16.0D以内の時だけ発動
        // これで「近くなら殴る、少し離れたら回る」という使い分けができる
        return distanceSq > 9.0D && distanceSq < 25.0D;
    }

    @Override
    public void stop() {
        this.attackStep = 0;
        this.mob.setSpinning(false);
        this.mob.getNavigation().stop();
        // 一度使ったら200チック（10秒間）は回転禁止だ！
        this.globalCooldown = 200;
    }

    @Override
    public void tick() {
        if (target == null) return;

        if (attackStep == 1) { // 開始
            this.mob.getLookControl().setLookAt(target);
            this.mob.getNavigation().moveTo(target, 1.2D);
            attackTimer++;
            if (attackTimer >= 17) {
                attackStep = 2;
                attackTimer = 0;
            }
        }
        else if (attackStep == 2) { // 回転中
            // --- ここが「物理的回転」の心臓部だ ---
            // 1チックごとに 72度回転させれば、5チック（0.25秒）で1回転する計算だ
            float nextYRot = this.mob.getYRot() + 72.0F;
            this.mob.setYRot(nextYRot);
            this.mob.setYBodyRot(nextYRot); // 体の向きも同期させる

            // ターゲットへにじり寄る
            this.mob.getNavigation().moveTo(target, 1.0D);
            attackTimer++;

            // ダメージ判定
            if (attackTimer % 5 == 0) {
                this.mob.level().getEntitiesOfClass(LivingEntity.class, this.mob.getBoundingBox().inflate(1.5D)).forEach(e -> {
                    if (e != mob && !mob.isAlliedTo(e)) {
                        e.hurt(this.mob.damageSources().mobAttack(this.mob), 4.0F);
                    }
                });
            }

            if (attackTimer >= 40) { // 2秒で終了
                attackStep = 3;
                attackTimer = 0;
            }
        }
        else if (attackStep == 3) { // 終了
            attackTimer++;
            if (attackTimer >= 15) this.stop();
        }
    }
}