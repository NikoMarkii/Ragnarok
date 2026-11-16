package com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.VolcanicSerpent;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class VolcanicSerpentPartEntity extends PartEntity<VolcanicSerpentHeadEntity> implements GeoEntity {
    private final int index;
    private final boolean isTail;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private float roll;

    public VolcanicSerpentPartEntity(VolcanicSerpentHeadEntity parent, int index, boolean isTail) {
        super(parent);
        this.index = index;
        this.isTail = isTail;
        this.noPhysics = true;
        this.setNoGravity(true);
        this.refreshDimensions();
    }

    public void updatePose(Vec3 position, Vec3 forward, float roll) {
        xo = xOld = getX();
        yo = yOld = getY();
        zo = zOld = getZ();
        yRotO = getYRot();
        xRotO = getXRot();

        setPos(position);
        if (forward.lengthSqr() > 1.0E-10) {
            if (forward.horizontalDistanceSqr() > 1.0E-10) {
                setYRot((float) (Mth.atan2(-forward.x, forward.z) * Mth.RAD_TO_DEG));
            }
            setXRot((float) (-Mth.atan2(forward.y, forward.horizontalDistance()) * Mth.RAD_TO_DEG));
        }
        this.roll = roll;
        tickCount = getParent().tickCount;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return isTail ? EntityDimensions.scalable(1.5F, 1.5F)
                : EntityDimensions.scalable(2.2F, 2.2F);
    }

    public int getIndex() {
        return index;
    }

    public boolean isTail() {
        return isTail;
    }

    public float getRoll() {
        return roll;
    }

    public void setRoll(float roll) {
        this.roll = roll;
    }

    @Override
    public boolean isAttackable() {
        return !isRemoved() && !getParent().isRemoved() && getParent().isAlive();
    }

    @Override
    public boolean isPickable() {
        return isAttackable();
    }

    @Override
    public boolean canBeHitByProjectile() {
        return isAttackable();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide || getParent() == null) return false;
        return getParent().hurtFromPart(this, source, amount);
    }

    @Override
    public boolean is(Entity entity) {
        return this == entity || entity == getParent();
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}
}