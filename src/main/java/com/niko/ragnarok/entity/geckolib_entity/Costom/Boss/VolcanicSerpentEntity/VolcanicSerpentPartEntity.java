package com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.VolcanicSerpentEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public abstract class VolcanicSerpentPartEntity extends VolcanicSerpentBaseEntity {
    private UUID parentUUID;
    private double followDistance;

    private static final float MAX_PART_TURN = 7.0F;
    private static final float FOLLOW_RATIO = 0.22F;

    protected VolcanicSerpentPartEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public void setParentUUID(UUID parentUUID) {
        this.parentUUID = parentUUID;
    }

    public UUID getParentUUID() {
        return this.parentUUID;
    }

    public void setFollowDistance(double followDistance) {
        this.followDistance = Math.max(0.0D, followDistance);
    }

    public double getFollowDistance() {
        return this.followDistance;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide || this.parentUUID == null) return;
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        Entity entity = serverLevel.getEntity(this.parentUUID);
        if (!(entity instanceof VolcanicSerpentHeadEntity head) || !head.isAlive()) return;

        Vec3 target = head.getPositionAtDistance(this.followDistance);
        float targetYaw = head.getYawAtDistance(this.followDistance);
        float targetPitch = head.getPitchAtDistance(this.followDistance);

        float currentYaw = this.getYRot();
        float yawDelta = Mth.wrapDegrees(targetYaw - currentYaw);
        float turnStep = Mth.clamp(yawDelta, -MAX_PART_TURN, MAX_PART_TURN);
        float smoothedYaw = currentYaw + turnStep * FOLLOW_RATIO;

        float currentPitch = this.getXRot();
        float pitchDelta = Mth.wrapDegrees(targetPitch - currentPitch);
        float pitchStep = Mth.clamp(pitchDelta, -MAX_PART_TURN, MAX_PART_TURN);
        float smoothedPitch = currentPitch + pitchStep * FOLLOW_RATIO;

        float currentRoll = this.getSerpentRoll();
        float targetRoll = head.getSerpentRoll();

        float rollDelta = Mth.wrapDegrees(targetRoll - currentRoll);
        float rollStep = Mth.clamp(rollDelta, -MAX_PART_TURN, MAX_PART_TURN);
        float smoothedRoll = currentRoll + rollStep * FOLLOW_RATIO;

        this.setPos(target.x, target.y, target.z);
        this.setYRot(smoothedYaw);
        this.setXRot(smoothedPitch);
        this.setSerpentRoll(smoothedRoll);

        this.yRotO = smoothedYaw;
        this.xRotO = smoothedPitch;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.parentUUID != null) tag.putUUID("ParentUUID", this.parentUUID);
        tag.putDouble("FollowDistance", this.followDistance);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.parentUUID = tag.hasUUID("ParentUUID") ? tag.getUUID("ParentUUID") : null;
        this.followDistance = tag.getDouble("FollowDistance");
    }
}