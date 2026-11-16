package com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.VolcanicSerpentEntity;

import com.niko.ragnarok.entity.RagnarokEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VolcanicSerpentHeadEntity extends VolcanicSerpentBaseEntity {
    public static final int DEFAULT_BODY_SEGMENTS = 5;
    public static final double SEGMENT_SPACING = 5.0D;
    private static final int HISTORY_LIMIT = 512;
    private static final double MOVE_SPEED = 0.105D;

    private static final float MAX_TURN_STEP = 7.0F;
    private static final float TURN_FOLLOW = 0.18F;
    private static final float VERTICAL_STEP = 0.12F;

    private final List<Vec3> movementHistory = new ArrayList<>();
    private final List<UUID> childIds = new ArrayList<>();
    private boolean segmentsInitialized;
    private int bodySegmentCount = DEFAULT_BODY_SEGMENTS;
    private float travelYaw;
    private float targetYaw;
    private double baseY;
    private int excursionTicks;
    private int nextExcursionTick = 260;

    public VolcanicSerpentHeadEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.noPhysics = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createPartAttributes()
                .add(Attributes.MAX_HEALTH, 1200.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.ARMOR, 20.0D);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        if (this.movementHistory.isEmpty()) {
            this.travelYaw = this.getYRot();
            this.targetYaw = this.getYRot();
            this.baseY = this.getY();
            this.seedHistory();
        }

        if (!this.segmentsInitialized && this.level() instanceof ServerLevel serverLevel) {
            this.spawnSegments(serverLevel);
        }

        this.moveSerpent();
        this.recordHistory();
    }

    private void seedHistory() {
        Vec3 forward = new Vec3(-Mth.sin(this.travelYaw * Mth.DEG_TO_RAD), 0,
                Mth.cos(this.travelYaw * Mth.DEG_TO_RAD));
        Vec3 current = this.position();
        for (int i = 0; i < 300; i++) {
            this.movementHistory.add(current.subtract(forward.scale(i * 0.12D)));
        }
    }

    private void spawnSegments(ServerLevel serverLevel) {
        for (int i = 0; i < this.bodySegmentCount; i++) {
            VolcanicSerpentBodyEntity body = RagnarokEntities.VOLCANIC_SERPENT_BODY.get().create(serverLevel);
            if (body == null) continue;
            body.setParentUUID(this.getUUID());
            body.setFollowDistance(SEGMENT_SPACING * (i + 1));
            Vec3 position = this.getPositionAtDistance(SEGMENT_SPACING * (i + 1));
            body.moveTo(position.x, position.y, position.z, this.getYRot(), this.getXRot());
            if (serverLevel.addFreshEntity(body)) this.childIds.add(body.getUUID());
        }

        VolcanicSerpentTailEntity tail = RagnarokEntities.VOLCANIC_SERPENT_TAIL.get().create(serverLevel);
        if (tail != null) {
            tail.setParentUUID(this.getUUID());
            tail.setFollowDistance(SEGMENT_SPACING * (this.bodySegmentCount + 1));
            Vec3 position = this.getPositionAtDistance(tail.getFollowDistance());
            tail.moveTo(position.x, position.y, position.z, this.getYRot(), this.getXRot());
            if (serverLevel.addFreshEntity(tail)) this.childIds.add(tail.getUUID());
        }
        this.segmentsInitialized = true;
    }

    private void moveSerpent() {
        int age = this.tickCount;
        float previousYaw = this.getYRot();

        float baseTurn = (float) Math.sin(age * 0.032D) * 18.0F;
        float sideWobble = (float) Math.sin(age * 0.09D) * 9.0F;
        this.targetYaw = this.travelYaw + baseTurn + sideWobble;

        Double lavaSurface = this.findNearbyLavaSurface();
        double targetY;

        if (this.excursionTicks > 0) {
            double phase = (this.excursionTicks / 120.0D) * Math.PI;
            double excursionHeight = 3.5D + Math.sin(phase) * 2.2D;
            targetY = (lavaSurface != null ? lavaSurface - 2.0D : this.baseY) + excursionHeight;
            this.excursionTicks--;
        } else if (age >= this.nextExcursionTick) {
            this.excursionTicks = 70 + this.random.nextInt(80);
            this.nextExcursionTick = age + 180 + this.random.nextInt(220);
            targetY = (lavaSurface != null ? lavaSurface - 2.0D : this.baseY) + 2.5D;
        } else if (lavaSurface != null) {
            targetY = lavaSurface - 2.0D + Math.sin(age * 0.035D) * 0.28D;
            this.baseY = targetY;
        } else {
            targetY = this.baseY + Math.sin(age * 0.018D) * 1.6D;
        }

        float yawDelta = Mth.wrapDegrees(this.targetYaw - this.travelYaw);
        float turnStep = Mth.clamp(yawDelta, -MAX_TURN_STEP, MAX_TURN_STEP);
        this.travelYaw += turnStep * TURN_FOLLOW;

        Vec3 forward = new Vec3(
                -Mth.sin(this.travelYaw * Mth.DEG_TO_RAD),
                0,
                Mth.cos(this.travelYaw * Mth.DEG_TO_RAD)
        ).normalize();

        double nextY = this.getY() + Mth.clamp(targetY - this.getY(), -VERTICAL_STEP, VERTICAL_STEP);
        Vec3 nextPosition = this.position()
                .add(forward.scale(MOVE_SPEED * (this.excursionTicks > 0 ? 1.35D : 1.0D)));
        nextPosition = new Vec3(nextPosition.x, nextY, nextPosition.z);

        Vec3 actualMotion = nextPosition.subtract(this.position());
        if (actualMotion.horizontalDistanceSqr() > 1.0E-7D) {
            this.travelYaw = (float) (Mth.atan2(-actualMotion.x, actualMotion.z) * Mth.RAD_TO_DEG);
        }

        float pitch = (float) (-Mth.atan2(actualMotion.y,
                Math.max(1.0E-4D, actualMotion.horizontalDistance())) * Mth.RAD_TO_DEG);

        this.setYRot(this.travelYaw);
        this.setXRot(Mth.clamp(pitch, -28.0F, 28.0F));
        this.yRotO = previousYaw;
        this.xRotO = this.getXRot();
        this.setPos(nextPosition.x, nextPosition.y, nextPosition.z);

        float curvature = Mth.wrapDegrees(this.travelYaw - previousYaw);
        this.setSerpentRoll(Mth.clamp(curvature * 0.04F, -0.22F, 0.22F));
    }

    private Double findNearbyLavaSurface() {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(
                Mth.floor(this.getX()), Mth.floor(this.getY() + 8.0D), Mth.floor(this.getZ()));
        for (int dy = 8; dy >= -16; dy--) {
            cursor.set(Mth.floor(this.getX()), Mth.floor(this.getY()) + dy, Mth.floor(this.getZ()));
            if (this.level().getFluidState(cursor).is(FluidTags.LAVA)
                    && !this.level().getFluidState(cursor.above()).is(FluidTags.LAVA)) {
                return cursor.getY() + 1.0D;
            }
        }
        return null;
    }

    private void recordHistory() {
        this.movementHistory.add(0, this.position());
        while (this.movementHistory.size() > HISTORY_LIMIT) {
            this.movementHistory.remove(this.movementHistory.size() - 1);
        }
    }

    public Vec3 getPositionAtDistance(double distance) {
        if (this.movementHistory.isEmpty()) return this.position();
        double travelled = 0.0D;
        for (int i = 1; i < this.movementHistory.size(); i++) {
            Vec3 newer = this.movementHistory.get(i - 1);
            Vec3 older = this.movementHistory.get(i);
            double step = newer.distanceTo(older);
            if (travelled + step >= distance && step > 1.0E-6D) {
                double alpha = (distance - travelled) / step;
                return newer.lerp(older, alpha);
            }
            travelled += step;
        }
        return this.movementHistory.get(this.movementHistory.size() - 1);
    }

    public float getYawAtDistance(double distance) {
        Vec3 nearHead = this.getPositionAtDistance(Math.max(0.0D, distance - 0.45D));
        Vec3 nearTail = this.getPositionAtDistance(distance + 0.45D);
        Vec3 forward = nearHead.subtract(nearTail);
        if (forward.horizontalDistanceSqr() < 1.0E-7D) return this.getYRot();
        return (float) (Mth.atan2(-forward.x, forward.z) * Mth.RAD_TO_DEG);
    }

    public float getPitchAtDistance(double distance) {
        Vec3 nearHead = this.getPositionAtDistance(Math.max(0.0D, distance - 0.45D));
        Vec3 nearTail = this.getPositionAtDistance(distance + 0.45D);
        Vec3 forward = nearHead.subtract(nearTail);
        return (float) (-Mth.atan2(forward.y, Math.max(1.0E-4D, forward.horizontalDistance())) * Mth.RAD_TO_DEG);
    }

    public void setBodySegmentCount(int count) {
        if (this.segmentsInitialized) return;
        this.bodySegmentCount = Mth.clamp(count, 1, 16);
    }

    public int getBodySegmentCount() {
        return this.bodySegmentCount;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("SegmentsInitialized", this.segmentsInitialized);
        tag.putInt("BodySegmentCount", this.bodySegmentCount);
        tag.putFloat("TravelYaw", this.travelYaw);
        tag.putFloat("TargetYaw", this.targetYaw);
        tag.putDouble("BaseY", this.baseY);
        tag.putInt("ExcursionTicks", this.excursionTicks);
        tag.putInt("NextExcursionTick", this.nextExcursionTick);

        ListTag historyTag = new ListTag();
        for (int i = 0; i < this.movementHistory.size(); i++) {
            Vec3 point = this.movementHistory.get(i);
            CompoundTag pointTag = new CompoundTag();
            pointTag.putDouble("X", point.x);
            pointTag.putDouble("Y", point.y);
            pointTag.putDouble("Z", point.z);
            historyTag.add(pointTag);
        }
        tag.put("MovementHistory", historyTag);

        ListTag childrenTag = new ListTag();
        for (UUID childId : this.childIds) {
            CompoundTag childTag = new CompoundTag();
            childTag.putUUID("UUID", childId);
            childrenTag.add(childTag);
        }
        tag.put("ChildEntities", childrenTag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.segmentsInitialized = tag.getBoolean("SegmentsInitialized");
        this.bodySegmentCount = tag.contains("BodySegmentCount")
                ? Mth.clamp(tag.getInt("BodySegmentCount"), 1, 16)
                : DEFAULT_BODY_SEGMENTS;
        this.travelYaw = tag.getFloat("TravelYaw");
        this.targetYaw = tag.contains("TargetYaw") ? tag.getFloat("TargetYaw") : this.travelYaw;
        this.baseY = tag.getDouble("BaseY");
        this.excursionTicks = tag.getInt("ExcursionTicks");
        this.nextExcursionTick = tag.getInt("NextExcursionTick");

        this.movementHistory.clear();
        ListTag historyTag = tag.getList("MovementHistory", Tag.TAG_COMPOUND);
        for (int i = 0; i < historyTag.size(); i++) {
            CompoundTag pointTag = historyTag.getCompound(i);
            this.movementHistory.add(new Vec3(pointTag.getDouble("X"), pointTag.getDouble("Y"), pointTag.getDouble("Z")));
        }

        this.childIds.clear();
        ListTag childrenTag = tag.getList("ChildEntities", Tag.TAG_COMPOUND);
        for (int i = 0; i < childrenTag.size(); i++) {
            CompoundTag childTag = childrenTag.getCompound(i);
            if (childTag.hasUUID("UUID")) this.childIds.add(childTag.getUUID("UUID"));
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason.shouldDestroy() && this.level() instanceof ServerLevel serverLevel) {
            for (UUID childId : this.childIds) {
                Entity child = serverLevel.getEntity(childId);
                if (child != null) child.discard();
            }
        }
        super.remove(reason);
    }
}