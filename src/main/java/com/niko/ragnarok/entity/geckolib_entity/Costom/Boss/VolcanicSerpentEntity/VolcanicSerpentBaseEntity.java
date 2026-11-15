package com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.VolcanicSerpentEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/** 共通の描画・同期基盤。攻撃やアニメーションは現段階では持たない。 */
public abstract class VolcanicSerpentBaseEntity extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Float> ROLL =
            SynchedEntityData.defineId(VolcanicSerpentBaseEntity.class, EntityDataSerializers.FLOAT);

    protected final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    protected VolcanicSerpentBaseEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setNoAi(true);
        this.setNoGravity(true);
        this.noPhysics = true;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createPartAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        // 移動は各部位の専用tick処理で行う。
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ROLL, 0.0F);
    }

    public float getSerpentRoll() {
        return this.entityData.get(ROLL);
    }

    protected void setSerpentRoll(float roll) {
        this.entityData.set(ROLL, roll);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 今回は静的モデルとして描画する。アニメーションは後工程で追加する。
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeLeashed(net.minecraft.world.entity.player.Player player) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("SerpentRoll", this.getSerpentRoll());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setSerpentRoll(tag.getFloat("SerpentRoll"));
    }
}
