package com.niko.ragnarok.entity.geckolib_entity.Costom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Cassowary extends Animal implements GeoEntity {
    private static final int ATTACK_HIT_TICK = 12;
    private static final int ATTACK_ANIMATION_TICKS = 20;
    private static final int SHIELD_DISABLE_TICKS = 100;
    private static final EntityDataAccessor<Integer> ATTACK_ANIMATION_REMAINING =
            SynchedEntityData.defineId(Cassowary.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public Cassowary(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 4;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new CassowaryAttackGoal(this, 1.25D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.FOLLOW_RANGE, 18.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2D);
    }

    public static boolean checkCassowarySpawnRules(EntityType<Cassowary> entityType, LevelAccessor level, net.minecraft.world.entity.MobSpawnType spawnType, BlockPos pos, net.minecraft.util.RandomSource random) {
        return level.getFluidState(pos.below()).isEmpty()
                && Animal.checkAnimalSpawnRules(entityType, level, spawnType, pos, random);
    }

    @Override
    public void tick() {
        super.tick();

        int attackAnimationTicks = this.getAttackAnimationTicks();
        if (attackAnimationTicks > 0) {
            this.entityData.set(ATTACK_ANIMATION_REMAINING, attackAnimationTicks - 1);
        }
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean didHurt = super.doHurtTarget(target);

        if (!this.level().isClientSide && target instanceof Player player && player.isBlocking()) {
            player.getCooldowns().addCooldown(Items.SHIELD, SHIELD_DISABLE_TICKS);
            this.level().broadcastEntityEvent(player, (byte) 30);
            player.stopUsingItem();
        }

        if (didHurt && target instanceof LivingEntity livingEntity) {
            livingEntity.knockback(0.7D, this.getX() - target.getX(), this.getZ() - target.getZ());
        }

        return didHurt;
    }

    public boolean isAttacking() {
        return this.getAttackAnimationTicks() > 0;
    }

    private void startAttackAnimation() {
        this.entityData.set(ATTACK_ANIMATION_REMAINING, ATTACK_ANIMATION_TICKS);
    }

    private int getAttackAnimationTicks() {
        return this.entityData.get(ATTACK_ANIMATION_REMAINING);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ATTACK_ANIMATION_REMAINING, 0);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob mate) {
        return null;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PARROT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.PARROT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PARROT_DEATH;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main_controller", 5, state -> {
            if (this.isAttacking()) {
                if (this.getAttackAnimationTicks() >= ATTACK_ANIMATION_TICKS - 1) {
                    state.getController().forceAnimationReset();
                }

                return state.setAndContinue(RawAnimation.begin().thenPlay("attack"));
            }

            if (state.isMoving()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            }

            return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    private static class CassowaryAttackGoal extends MeleeAttackGoal {
        private final Cassowary cassowary;
        @Nullable
        private LivingEntity animationTarget;
        private int animationTick;
        private boolean hasHit;

        public CassowaryAttackGoal(Cassowary cassowary, double speedModifier, boolean followingTargetEvenIfNotSeen) {
            super(cassowary, speedModifier, followingTargetEvenIfNotSeen);
            this.cassowary = cassowary;
        }

        @Override
        public void stop() {
            super.stop();
            this.animationTarget = null;
            this.animationTick = 0;
            this.hasHit = false;
        }

        @Override
        public void tick() {
            super.tick();

            if (this.animationTarget == null) {
                return;
            }

            this.animationTick++;
            this.mob.getNavigation().stop();
            this.mob.getLookControl().setLookAt(this.animationTarget, 30.0F, 30.0F);

            if (!this.hasHit && this.animationTick >= ATTACK_HIT_TICK) {
                this.hasHit = true;
                double distanceToTargetSqr = this.mob.distanceToSqr(this.animationTarget.getX(), this.animationTarget.getY(), this.animationTarget.getZ());

                if (this.animationTarget.isAlive() && distanceToTargetSqr <= this.getAttackReachSqr(this.animationTarget)) {
                    this.mob.doHurtTarget(this.animationTarget);
                }
            }

            if (this.animationTick >= ATTACK_ANIMATION_TICKS) {
                this.animationTarget = null;
                this.animationTick = 0;
                this.hasHit = false;
            }
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity target, double distanceToTargetSqr) {
            if (this.animationTarget == null && distanceToTargetSqr <= this.getAttackReachSqr(target) && this.getTicksUntilNextAttack() <= 0) {
                this.resetAttackCooldown();
                this.animationTarget = target;
                this.animationTick = 0;
                this.hasHit = false;
                this.cassowary.startAttackAnimation();
            }
        }

        @Override
        protected int adjustedTickDelay(int ticks) {
            return super.adjustedTickDelay(ticks);
        }
    }

    @Override
    public boolean isFood(net.minecraft.world.item.ItemStack stack) {
        return false;
    }
}
