package com.niko.ragnarok.entity.geckolib_entity.Costom;

import com.niko.ragnarok.entity.Boss_Monster;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.List;

/**
 * ガレオス - 中ボス
 * attack1   : 右腕 発生25tick 長さ45tick
 * attack1_2 : 左腕 発生25tick 長さ45tick
 * attack2   : 両腕叩きつけ 発生25tick 長さ45tick
 */
public class GaleosEntity extends Boss_Monster implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Integer> ATTACK_STATE =
            SynchedEntityData.defineId(GaleosEntity.class, EntityDataSerializers.INT);

    private static final int ATK_HIT   = 25;
    private static final int ATK_TOTAL = 45;

    private int previousAttackState = 0;

    public GaleosEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH,      120.0D)
                .add(Attributes.MOVEMENT_SPEED,    0.24D)
                .add(Attributes.ATTACK_DAMAGE,     10.0D)
                .add(Attributes.FOLLOW_RANGE,      32.0D)
                .add(Attributes.ARMOR,              8.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ATTACK_STATE, 0);
    }

    public int getAttackState()       { return this.entityData.get(ATTACK_STATE); }
    public void setAttackState(int s) { this.entityData.set(ATTACK_STATE, s);     }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new GaleosAttackGoal(this, 1.05D));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 12.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.HUSK_AMBIENT; }
    @Override
    protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.HUSK_HURT; }
    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.HUSK_DEATH; }
    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        this.playSound(SoundEvents.RAVAGER_STEP, 1.0F, 1.0F);
    }

    // ──────────────────────────────────────────
    // AttackGoal
    // ──────────────────────────────────────────
    static class GaleosAttackGoal extends Goal {

        private final GaleosEntity mob;
        private final double speed;
        private LivingEntity target;

        private int attackTimer = 0;
        private int cooldown    = 0;

        private static final double ATTACK_START_SQ = 9.0D; // 3ブロック以内で攻撃開始

        GaleosAttackGoal(GaleosEntity mob, double speed) {
            this.mob = mob;
            this.speed = speed;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = mob.getTarget();
            return t != null && t.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity t = mob.getTarget();
            return t != null && t.isAlive();
        }

        @Override
        public void start() {
            this.target = mob.getTarget();
            this.attackTimer = 0;
            this.cooldown = 0;
        }

        @Override
        public void stop() {
            mob.setAttackState(0);
            attackTimer = 0;
            mob.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void tick() {
            LivingEntity t = mob.getTarget();
            if (t == null || !t.isAlive()) {
                mob.setAttackState(0);
                return;
            }
            this.target = t;
            mob.getLookControl().setLookAt(t, 30F, 30F);

            if (cooldown > 0) {
                cooldown--;
                mob.getNavigation().moveTo(t, speed);
                return;
            }

            if (mob.getAttackState() > 0) {
                attackTimer++;
                mob.getNavigation().stop();
                executeAttack(t);
                return;
            }

            double distSq = mob.distanceToSqr(t);
            if (distSq <= ATTACK_START_SQ) {
                mob.getNavigation().stop();
                startAttack();
            } else {
                mob.getNavigation().moveTo(t, speed);
            }
        }

        private void startAttack() {
            int roll = mob.random.nextInt(3);
            mob.setAttackState(roll + 1); // 1,2,3
            attackTimer = 0;
        }

        private void executeAttack(LivingEntity t) {
            int atk = mob.getAttackState();

            if (attackTimer == ATK_HIT) {
                switch (atk) {
                    case 1 -> doArmSwipe(1.0);   // attack1：右腕
                    case 2 -> doArmSwipe(-1.0);  // attack1_2：左腕
                    case 3 -> doDoubleSlam();    // attack2：両腕叩きつけ
                }
            }

            if (attackTimer >= ATK_TOTAL) {
                finishAttack(30);
            }
        }

        private void doArmSwipe(double side) {
            Vec3 look = mob.getLookAngle();
            AABB box = mob.getBoundingBox()
                    .move(look.x * 2.5, 0, look.z * 2.5)
                    .inflate(3.0, 2.0, 3.0);

            for (LivingEntity e : getHittableEntities(box)) {
                e.hurt(mob.damageSources().mobAttack(mob),
                        (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE));

                Vec3 kb = e.position().subtract(mob.position()).normalize().scale(1.8);
                e.setDeltaMovement(kb.x, 0.5, kb.z);
                e.hurtMarked = true;
            }
            mob.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.8F);
        }

        private void doDoubleSlam() {
            AABB box = mob.getBoundingBox().inflate(4.0, 2.0, 4.0);

            for (LivingEntity e : getHittableEntities(box)) {
                e.hurt(mob.damageSources().mobAttack(mob),
                        (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.3F);

                Vec3 kb = e.position().subtract(mob.position()).normalize().scale(2.2);
                e.setDeltaMovement(kb.x, 0.7, kb.z);
                e.hurtMarked = true;
            }
            mob.playSound(SoundEvents.GENERIC_EXPLODE, 1.5F, 0.6F);
        }

        private List<LivingEntity> getHittableEntities(AABB box) {
            return mob.level().getEntitiesOfClass(LivingEntity.class, box,
                    e -> e != mob && e.isAlive() && !(e instanceof GaleosEntity));
        }

        private void finishAttack(int cd) {
            attackTimer = 0;
            cooldown = cd;
            mob.setAttackState(0);
        }
    }

    // ──────────────────────────────────────────
    // アニメーション
    // ──────────────────────────────────────────
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "base_controller", 5, state -> {
            if (this.getAttackState() > 0) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
            }
            if (state.isMoving()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));

        controllers.add(new AnimationController<>(this, "action_controller", 3, state -> {
            int atk = this.getAttackState();
            if (atk > 0) {
                if (this.previousAttackState != atk) {
                    state.getController().forceAnimationReset();
                    this.previousAttackState = atk;
                }
                String animName = switch (atk) {
                    case 1 -> "attack1";
                    case 2 -> "attack1_2";
                    case 3 -> "attack2";
                    default -> "idle";
                };
                return state.setAndContinue(RawAnimation.begin().thenPlay(animName));
            }
            if (this.previousAttackState != 0) {
                this.previousAttackState = 0;
                state.getController().forceAnimationReset();
            }
            return software.bernie.geckolib.core.object.PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }
}
