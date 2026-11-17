package com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.Volcanic_Serpent;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class VolcanicSerpentPartEntity
        extends LivingEntity implements GeoEntity {

    private static final float FOLLOW_ANGLE = 270.0F;

    private final AnimatableInstanceCache geoCache =
            GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Optional<UUID>> PARENT_UUID =
            SynchedEntityData.defineId(
                    VolcanicSerpentPartEntity.class,
                    EntityDataSerializers.OPTIONAL_UUID
            );

    private static final EntityDataAccessor<Integer> PARENT_ENTITY_ID =
            SynchedEntityData.defineId(
                    VolcanicSerpentPartEntity.class,
                    EntityDataSerializers.INT
            );

    private static final EntityDataAccessor<Boolean> TAIL =
            SynchedEntityData.defineId(
                    VolcanicSerpentPartEntity.class,
                    EntityDataSerializers.BOOLEAN
            );

    private static final EntityDataAccessor<Integer> BODY_INDEX =
            SynchedEntityData.defineId(
                    VolcanicSerpentPartEntity.class,
                    EntityDataSerializers.INT
            );

    private static final EntityDataAccessor<Float> FOLLOW_DISTANCE =
            SynchedEntityData.defineId(
                    VolcanicSerpentPartEntity.class,
                    EntityDataSerializers.FLOAT
            );

    public VolcanicSerpentPartEntity(
            EntityType<? extends VolcanicSerpentPartEntity> type,
            Level level
    ) {
        super(type, level);

        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();

        this.entityData.define(PARENT_UUID, Optional.empty());
        this.entityData.define(PARENT_ENTITY_ID, -1);
        this.entityData.define(TAIL, false);
        this.entityData.define(BODY_INDEX, 0);
        this.entityData.define(FOLLOW_DISTANCE, 1.0F);
    }

    // --------------------------------------------------
    // Parent
    // --------------------------------------------------

    public void setParent(LivingEntity parent) {
        this.entityData.set(
                PARENT_UUID,
                Optional.of(parent.getUUID())
        );
        this.entityData.set(PARENT_ENTITY_ID, parent.getId());
    }

    @Nullable
    public UUID getParentUUID() {
        return this.entityData.get(PARENT_UUID).orElse(null);
    }

    @Nullable
    public Entity getParent() {
        UUID uuid = this.getParentUUID();

        if (uuid == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        return serverLevel.getEntity(uuid);
    }

    // --------------------------------------------------
    // Part information
    // --------------------------------------------------

    public void setTail(boolean tail) {
        this.entityData.set(TAIL, tail);
    }

    public boolean isTail() {
        return this.entityData.get(TAIL);
    }

    public void setBodyIndex(int index) {
        this.entityData.set(BODY_INDEX, index);
    }

    public int getBodyIndex() {
        return this.entityData.get(BODY_INDEX);
    }

    public void setFollowDistance(float distance) {
        this.entityData.set(FOLLOW_DISTANCE, distance);
    }

    public float getFollowDistance() {
        return this.entityData.get(FOLLOW_DISTANCE);
    }

    // --------------------------------------------------
    // Initial position
    // --------------------------------------------------

    public void setInitialPartPos(Entity parent) {
        double angle = Math.toRadians(
                parent.getYRot() + FOLLOW_ANGLE
        );
        double pitch = Math.toRadians(parent.getXRot());

        double distance = this.getFollowDistance();

        double desiredHorizontalDistance = Math.cos(pitch) * distance;
        double x = parent.getX() + Math.cos(angle) * desiredHorizontalDistance;
        double y = parent.getY() + Math.sin(pitch) * distance;
        double z = parent.getZ() + Math.sin(angle) * desiredHorizontalDistance;

        this.setPos(x, y, z);
        this.setYRot(parent.getYRot());
        this.yBodyRot = parent.getYRot();
        this.yHeadRot = parent.getYRot();
        this.setXRot(parent.getXRot());
        this.updatePartHitbox();
    }

    // --------------------------------------------------
    // Following
    // --------------------------------------------------

    @Override
    public void tick() {
        super.tick();

        Entity trackedParent = this.level().getEntity(this.entityData.get(PARENT_ENTITY_ID));
        if (trackedParent instanceof LivingEntity livingParent) {
            this.hurtTime = livingParent.hurtTime;
            this.deathTime = livingParent.deathTime;
        }

        if (this.level().isClientSide) {
            return;
        }

        Entity parent = this.getParent();

        // 親が存在しなくなった場合はパーツを削除
        if (parent == null || parent.isRemoved()) {
            if (this.tickCount > 20) {
                this.discard();
            }
            return;
        }

        this.entityData.set(PARENT_ENTITY_ID, parent.getId());
        this.setNoGravity(true);

        // Use the parent's previous pose so each segment follows the path it took.
        double angle = Math.toRadians(
                parent.yRotO + FOLLOW_ANGLE
        );
        double pitch = Math.toRadians(parent.xRotO);

        double distance = this.getFollowDistance();

        double desiredHorizontalDistance = Math.cos(pitch) * distance;
        double x = parent.xo + Math.cos(angle) * desiredHorizontalDistance;
        double y = parent.yo + Math.sin(pitch) * distance;
        double z = parent.zo + Math.sin(angle) * desiredHorizontalDistance;

        this.setNoGravity(true);
        Vec3 followPosition = this.findClearFollowPosition(new Vec3(x, y, z), parent.yRotO);
        this.move(
                MoverType.SELF,
                followPosition.subtract(this.position())
        );

        this.updatePartHitbox();

        // Face the parent so bends propagate smoothly down the chain.
        double dx = parent.getX() - this.getX();
        double dy = parent.getY() - this.getY();
        double dz = parent.getZ() - this.getZ();

        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = horizontalDistance < 1.0E-4D
                ? parent.getYRot()
                : (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        float targetPitch = (float) (
                -Math.atan2(dy, horizontalDistance) * Mth.RAD_TO_DEG
        );

        this.setYRot(Mth.rotLerp(0.35F, this.getYRot(), targetYaw));
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
        this.setXRot(
                Mth.rotLerp(
                        0.35F,
                        this.getXRot(),
                        targetPitch
                )
        );
        this.updatePartHitbox();
    }

    // --------------------------------------------------
    // Damage
    // --------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity parent = this.getParent();

        if (parent instanceof LivingEntity livingParent) {
            return livingParent.hurt(source, amount);
        }

        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.is(DamageTypes.IN_WALL)
                || source.is(DamageTypes.FALL)
                || super.isInvulnerableTo(source);
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }

    @Override
    public boolean is(Entity entity) {
        return this == entity || this.getParent() == entity;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEAD;
    }

    // --------------------------------------------------
    // Save / Load
    // --------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        UUID parentUUID = this.getParentUUID();

        if (parentUUID != null) {
            tag.putUUID("ParentUUID", parentUUID);
        }

        tag.putBoolean("IsTail", this.isTail());
        tag.putInt("BodyIndex", this.getBodyIndex());
        tag.putFloat("FollowDistance", this.getFollowDistance());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.hasUUID("ParentUUID")) {
            this.entityData.set(
                    PARENT_UUID,
                    Optional.of(tag.getUUID("ParentUUID"))
            );
        }

        this.setTail(tag.getBoolean("IsTail"));
        this.setBodyIndex(tag.getInt("BodyIndex"));
        this.setFollowDistance(tag.getFloat("FollowDistance"));
    }

    private void updatePartHitbox() {
        double length = this.isTail() ? 9.5D : 5.25D;
        double width = 2.9D;
        double height = 2.6D;

        double yaw = Math.toRadians(this.getYRot());

        double halfLength = length / 2.0D;
        double halfWidth = width / 2.0D;

        // 回転した長方形を覆うAABBの半径
        double halfX =
                Math.abs(Math.sin(yaw)) * halfLength
                        + Math.abs(Math.cos(yaw)) * halfWidth;

        double halfZ =
                Math.abs(Math.cos(yaw)) * halfLength
                        + Math.abs(Math.sin(yaw)) * halfWidth;

        this.setBoundingBox(new net.minecraft.world.phys.AABB(
                this.getX() - halfX,
                this.getY(),
                this.getZ() - halfZ,
                this.getX() + halfX,
                this.getY() + height,
                this.getZ() + halfZ
        ));
    }

    private Vec3 findClearFollowPosition(Vec3 target, float parentYaw) {
        double yawRadians = parentYaw * Mth.DEG_TO_RAD;
        Vec3 side = new Vec3(
                Math.cos(yawRadians),
                0.0D,
                Math.sin(yawRadians)
        );
        Vec3[] offsets = {
                Vec3.ZERO,
                side.scale(1.25D),
                side.scale(-1.25D),
                new Vec3(0.0D, 1.25D, 0.0D),
                new Vec3(0.0D, -1.25D, 0.0D),
                side.scale(2.5D),
                side.scale(-2.5D)
        };

        for (Vec3 offset : offsets) {
            Vec3 candidate = target.add(offset);
            net.minecraft.world.phys.AABB candidateBounds =
                    this.getBoundingBox().move(candidate.subtract(this.position()));
            if (this.level().noCollision(this, candidateBounds)) {
                return candidate;
            }
        }

        return this.position();
    }

    // --------------------------------------------------
    // LivingEntity abstract methods
    // --------------------------------------------------

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return List.of();
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        // パーツは装備を持たない
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
