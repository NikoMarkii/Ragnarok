package com.niko.ragnarok.entity.costom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class Scorpion extends Monster {
    public static final int STING_TICK = 14; // 尻尾で刺すタイミング
    public static final int ATTACK_ANIM_TICKS = 21;

    private static final EntityDataAccessor<Boolean> ATTACKING =
            SynchedEntityData.defineId(Scorpion.class, EntityDataSerializers.BOOLEAN);

    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState attackAnimationState = new AnimationState();
    public int attackAnimationTimeout = 0;
    public boolean hasDealtStingDamage = false;

    public Scorpion(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setHealth(20f);
    }
    public static AttributeSupplier.Builder createMobAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) setupAnimationStates();
    }

    private void setupAnimationStates() {
        if (isAttacking()) {
            if (!attackAnimationState.isStarted()) {
                attackAnimationState.startIfStopped(this.tickCount);
                idleAnimationState.stop();
            }
        } else {
            if (!idleAnimationState.isStarted()) idleAnimationState.startIfStopped(this.tickCount);
            if (attackAnimationState.isStarted()) attackAnimationState.stop();
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ATTACKING, false);
    }

    public void setAttacking(boolean attacking) {
        this.entityData.set(ATTACKING, attacking);
    }

    public boolean isAttacking() {
        return this.entityData.get(ATTACKING);
    }

    public void performStingAttack(LivingEntity target) {
        if (target != null && target.isAlive() && this.distanceTo(target) < 3f) { // 尻尾のリーチを広げる
            // ダメージA
            float damage = (float)this.getAttribute(Attributes.ATTACK_DAMAGE).getValue();
            target.hurt(this.damageSources().mobAttack(this), damage);

            // ノックバック
            target.knockback(0.5F, this.getX() - target.getX(), this.getZ() - target.getZ());

            // 毒効果
            int poisonTicks = switch (this.level().getDifficulty()) {
                case NORMAL -> 7 * 20;
                case HARD -> 15 * 20;
                default -> 0;
            };
            if (poisonTicks > 0) {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, poisonTicks, 0), this);
            }
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.4F));
        this.goalSelector.addGoal(3, new ScorpionAttackGoal(this, 1.5D, true));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.3D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new ScorpionTargetGoal<>(this, Player.class));
        this.targetSelector.addGoal(3, new ScorpionTargetGoal<>(this, IronGolem.class));
    }

    static class ScorpionTargetGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
        public ScorpionTargetGoal(Scorpion scorpion, Class<T> clazz) {
            super(scorpion, clazz, true);
        }
    }
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SPIDER_AMBIENT;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundEvents.SPIDER_HURT;
    }
    @Override
    protected void playStepSound(BlockPos pos, BlockState blockIn) {
        float volume = 0.4F + this.random.nextFloat() * 0.2F;
        float pitch = 0.8F + this.random.nextFloat() * 0.2F;

        this.level().playSound(
                null,
                this.getX(), this.getY(), this.getZ(),
                SoundEvents.SPIDER_STEP,
                SoundSource.HOSTILE,   // ★ ここも同様に
                volume,
                pitch
        );
    }
    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SPIDER_DEATH;
    }
    class ScorpionAttackGoal extends MeleeAttackGoal {
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

            // 1. 常にターゲットを注視し、追いかける
            scorpion.getLookControl().setLookAt(target, 30.0F, 30.0F);

            double distanceSq = scorpion.distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ());
            double reachSq = getAttackReachSqr(target);

            if (attackCooldown > 0) attackCooldown--;

            // 2. 攻撃中でない場合のみ、移動と攻撃開始の判定を行う
            if (!scorpion.isAttacking()) {
                if (distanceSq <= reachSq && attackCooldown <= 0) {
                    startAttackSequence(target);
                } else {
                    scorpion.getNavigation().moveTo(target, speedModifier);
                }
            }
            if (scorpion.isAttacking()) {
                scorpion.getNavigation().stop();
                updateAttackSequence(target);
            }
        }
        private void startAttackSequence(LivingEntity target) {
            scorpion.setAttacking(true);
            scorpion.attackAnimationTimeout = Scorpion.ATTACK_ANIM_TICKS;
            scorpion.hasDealtStingDamage = false;

            scorpion.attackAnimationState.start(scorpion.tickCount);
        }
        private void updateAttackSequence(LivingEntity target) {
            scorpion.attackAnimationTimeout--;

            if (scorpion.attackAnimationTimeout == Scorpion.ATTACK_ANIM_TICKS - Scorpion.STING_TICK
                    && !scorpion.hasDealtStingDamage) {
                scorpion.performStingAttack(target);
                scorpion.hasDealtStingDamage = true;
            }

            // アニメーション終了
            if (scorpion.attackAnimationTimeout <= 0) {
                scorpion.setAttacking(false);
                scorpion.hasDealtStingDamage = false;
                scorpion.attackAnimationState.stop();
                this.attackCooldown = 5;
            }
        }
    }
}


