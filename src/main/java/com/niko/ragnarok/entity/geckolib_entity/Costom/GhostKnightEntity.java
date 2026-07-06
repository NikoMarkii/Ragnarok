package com.niko.ragnarok.entity.geckolib_entity.Costom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.List;

public class GhostKnightEntity extends Monster implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Integer> ATTACK_STATE =
            SynchedEntityData.defineId(GhostKnightEntity.class, EntityDataSerializers.INT);

    private static final int ATK1_TOTAL = 45;
    private static final int ATK1_HIT   = 25;

    public GhostKnightEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        // サーバー側でのみアイテムをセット（クライアントには同期される）
        if (!level.isClientSide()) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH,      45.0D)
                .add(Attributes.MOVEMENT_SPEED,   0.25D)
                .add(Attributes.ATTACK_DAMAGE,     7.0D)
                .add(Attributes.FOLLOW_RANGE,      24.0D)
                .add(Attributes.ARMOR,              6.0D);
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
        this.goalSelector.addGoal(1, new KnightAttackGoal(this));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 12.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this, Player.class, true));
    }

    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.SKELETON_AMBIENT; }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.SKELETON_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.SKELETON_DEATH; }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        this.playSound(SoundEvents.SKELETON_STEP, 1.0F, 1.0F);
    }

    // ──────────────────────────────────────────
    // KnightAttackGoal
    // ──────────────────────────────────────────
    static class KnightAttackGoal extends Goal {

        private final GhostKnightEntity mob;
        private LivingEntity target;
        private int attackTimer = 0;
        private int cooldown    = 0;

        private static final double ATTACK_START_SQ = 9.0D; // 3ブロック以内で攻撃開始

        KnightAttackGoal(GhostKnightEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
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
                mob.getNavigation().moveTo(t, 1.0D);
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
                mob.getNavigation().moveTo(t, 1.0D);
            }
        }

        private void startAttack() {
            mob.setAttackState(1);
            attackTimer = 0;
        }

        private void executeAttack(LivingEntity t) {
            if (mob.getAttackState() == 1) {

                if (attackTimer == ATK1_HIT) {
                    doSlash(t);
                }

                if (attackTimer >= ATK1_TOTAL) {
                    finishAttack(25);
                }
            }
        }

        private void doSlash(LivingEntity primary) {
            if (!primary.isAlive()) return;

            AABB box = mob.getBoundingBox().inflate(2.5, 1.5, 2.5);
            if (!box.intersects(primary.getBoundingBox())) return; // ターゲットが範囲内かだけ確認

            primary.invulnerableTime = 0;
            primary.hurt(mob.damageSources().mobAttack(mob),
                    (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE));

            Vec3 kb = primary.position().subtract(mob.position()).normalize().scale(1.2);
            primary.setDeltaMovement(kb.x, 0.3, kb.z);
            primary.hurtMarked = true;

            mob.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.8F);
        }
        private void finishAttack(int cd) {
            attackTimer = 0;
            cooldown    = cd;
            mob.setAttackState(0);
        }
    }

    // ──────────────────────────────────────────
    // GeckoLib アニメーション
    // ──────────────────────────────────────────
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, state -> {

            if (this.getAttackState() == 1) {
                return state.setAndContinue(
                        RawAnimation.begin().thenPlay("attack1"));
            }

            if (state.isMoving()) {
                return state.setAndContinue(
                        RawAnimation.begin().thenLoop("walk"));
            }

            return state.setAndContinue(
                    RawAnimation.begin().thenLoop("idle"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }
}
