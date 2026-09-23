package com.niko.ragnarok.entity.geckolib_entity.Costom;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;


public class SakuBotEntity extends Monster implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SakuBotEntity(EntityType<? extends Monster> type, Level level) {
        super(type,level);
    }
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this,1.0D,false));
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
            .add(Attributes.ATTACK_DAMAGE,     7.0D)
            .add(Attributes.FOLLOW_RANGE,      24.0D)
            .add(Attributes.ARMOR,              6.0D);
    }
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller",5,state -> {
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
}
