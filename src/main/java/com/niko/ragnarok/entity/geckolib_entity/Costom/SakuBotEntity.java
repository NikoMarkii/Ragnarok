package com.niko.ragnarok.entity.geckolib_entity.Costom;

import com.niko.ragnarok.entity.geckolib_entity.model.renderer.SakubotRenderer;
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
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
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


public class SakuBotEntity extends Monster implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Integer> ATTACK_STATE =
            SynchedEntityData.defineId(SakuBotEntity.class, EntityDataSerializers.INT);

    private static final int ATK1_TOTAL = 25;
    private static final int ATK1_HIT   = 13;

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ATTACK_STATE, 0);
    }

    public int getAttackState()       { return this.entityData.get(ATTACK_STATE); }
    public void setAttackState(int s) { this.entityData.set(ATTACK_STATE, s);     }

    public SakuBotEntity(EntityType<? extends Monster> type, Level level) {
        super(type,level);
    }
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new SakuBotAttackGoal(this));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 12.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this, Player.class, true));
    }

public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMobAttributes()
            .add(Attributes.MAX_HEALTH,      50.0D)
            .add(Attributes.MOVEMENT_SPEED,   0.25D)
            .add(Attributes.ATTACK_DAMAGE,     8.0D)
            .add(Attributes.FOLLOW_RANGE,      24.0D)
            .add(Attributes.ARMOR,              8.0D);
    }
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller",5,state -> {
            if (this.getAttackState() == 1) {
                return state.setAndContinue(
                        RawAnimation.begin().thenPlay("attack1"));
            }

            if(state.isMoving()) {
                return state.setAndContinue(
                        RawAnimation.begin().thenLoop("walk")
                );
            }
            return state.setAndContinue(
                    RawAnimation.begin().thenLoop("idle")
            );
        }));
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.ANVIL_LAND;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ANVIL_DESTROY;
    }

    @Override
    protected void dropAllDeathLoot(DamageSource damageSource) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

static class SakuBotAttackGoal extends Goal {
private final SakuBotEntity mob ;
    private LivingEntity target;
    private int attackTimer = 0;
    private int cooldown    = 0;

    private static final double ATTACK_START_SQ = 9.0D;

SakuBotAttackGoal(SakuBotEntity mob) {
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
    @Override
    public boolean requiresUpdateEveryTick() { return true; }
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
}
