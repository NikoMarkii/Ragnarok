package com.niko.ragnarok.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public abstract class Boss_Monster extends Monster {

    private static final float MAX_DAMAGE_PER_HIT = 30.0F;
    private static final int DAMAGE_REDUCTION_DURATION = 10;
    private static final float DAMAGE_REDUCTION_FACTOR = 0.5F;
    private static final int REGEN_INTERVAL = 20;
    private static final float REGEN_AMOUNT = 50.0F;

    private int damageReductionTimer = 0;
    private int regenTimer = 0;

    private static final int MAX_ADAPTATION_STACKS = 10;          // 最大スタック数
    private static final float ADAPTATION_REDUCTION_PER_STACK = 0.10F; // 1スタックあたりのカット率 (0.05 = 5%)
    private static final int ADAPTATION_DECAY_TIME = 100;         // 耐性が1段階落ちるまでの時間 (100tick = 5秒)

    private int adaptationStacks = 0;
    private int adaptationDecayTimer = 0;

    protected Boss_Monster(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void checkDespawn() {
        // デスポーン処理を無効化しつつ、AIがサボらないようにアイドル時間をリセットし続ける
        this.noActionTime = 0;
    }

    // サブクラスで待機中かどうかを返す（待機中は自動回復しない）
    protected boolean isInStandbyState() {
        return false; // デフォルトはfalse、サブクラスでオーバーライド
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // /killコマンドや即死ダメージはキャップ・軽減しない
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)
                || source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) {
            return super.hurt(source, amount);
        }

        float capped = Math.min(amount, MAX_DAMAGE_PER_HIT);

        // ── 追加：ダメージ適応による軽減処理 ──
        // 例: 5スタックなら 1.0 - (5 * 0.05) = 0.75倍 (25%カット)
        float adaptationMultiplier = 1.0F - (adaptationStacks * ADAPTATION_REDUCTION_PER_STACK);
        // 念のため、ダメージが0以下にならないよう下限を設ける（最低でも10%は通るように）
        capped *= Math.max(0.1F, adaptationMultiplier);

        // 既存の連続ヒット軽減
        if (damageReductionTimer > 0) {
            capped *= DAMAGE_REDUCTION_FACTOR;
        }

        boolean result = super.hurt(source, capped);

        if (result) {
            damageReductionTimer = DAMAGE_REDUCTION_DURATION;

            // ── 追加：攻撃を受けたので適応スタックを増加させ、減衰タイマーをリセット ──
            if (adaptationStacks < MAX_ADAPTATION_STACKS) {
                adaptationStacks++;
            }
            adaptationDecayTimer = ADAPTATION_DECAY_TIME;
        }

        return result;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (damageReductionTimer > 0) {
            damageReductionTimer--;
        }

        // ターゲットが存在しない、あるいはターゲットが既に死んでいる場合は「戦闘外」とする
        boolean isOutOfCombat = this.getTarget() == null || !this.getTarget().isAlive();

        // ── 追加：適応（耐性）の減衰処理 ──
        if (isOutOfCombat) {
            // 戦闘外になれば、適応スタックは即座にリセットされる
            adaptationStacks = 0;
            adaptationDecayTimer = 0;
        } else if (adaptationStacks > 0) {
            // 戦闘中だが攻撃を受けていない時間が続くと、徐々に耐性が落ちていく
            if (adaptationDecayTimer > 0) {
                adaptationDecayTimer--;
            } else {
                adaptationStacks--;
                // 次のスタックが減るまでの時間をセット
                adaptationDecayTimer = ADAPTATION_DECAY_TIME;
            }
        }

        // 待機中ではなく、戦闘外であり、生きているかつ体力が減っている場合
        if (!this.level().isClientSide()
                && !isInStandbyState()
                && isOutOfCombat
                && this.isAlive()
                && this.getHealth() < this.getMaxHealth()) {

            regenTimer++;
            if (regenTimer >= REGEN_INTERVAL) {
                regenTimer = 0;
                this.heal(REGEN_AMOUNT);
            }
        } else {
            regenTimer = 0;
        }
    }
}