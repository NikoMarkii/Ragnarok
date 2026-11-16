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

/** Volcanic Serpent head; owns movement history and its body parts. */
public class VolcanicSerpentHeadEntity extends VolcanicSerpentBaseEntity {
    public static final int DEFAULT_BODY_SEGMENTS = 5;
    /** Segment centers are closer than the model's 5.25-block body length so bends remain connected. */
    public static final double SEGMENT_SPACING = 4.0D;
    private static final int HISTORY_LIMIT = 512;
    private static final double MOVE_SPEED = 0.105D;

    private static final float MAX_TURN_STEP = 7.0F;
    private static final float TURN_FOLLOW = 0.18F;
    private static final double VERTICAL_STEP = 0.12D;

    private final List<Vec3> movementHistory = new ArrayList<>(); // newest first
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

    /** Smoothly steers the head and preserves the existing lava-surface swim/excursion behavior. */
    private void moveSerpent() {
        int age = this.tickCount;
        float previousYaw = this.getYRot();
        float previousPitch = this.getXRot();

        float broadTurn = (float) Math.sin(age * 0.0065D) * 0.48F;
        float smallWaver = (float) Math.sin(age * 0.021D) * 0.22F;
        this.targetYaw = this.travelYaw + broadTurn + smallWaver;

        Double lavaSurface = this.findNearbyLavaSurface();
        boolean excursionActive = this.excursionTicks > 0;
        if (!excursionActive && age >= this.nextExcursionTick) {
            this.excursionTicks = 100 + this.random.nextInt(70);
            this.nextExcursionTick = age + 300 + this.random.nextInt(260);
            excursionActive = true;
        }

        double targetY;
        if (excursionActive) {
            double phase = (this.excursionTicks / 170.0D) * Math.PI;
            double excursionHeight = 3.5D + Math.sin(phase) * 1.8D;
            targetY = (lavaSurface != null ? lavaSurface - 2.0D : this.baseY) + excursionHeight;
            this.excursionTicks--;
        } else if (lavaSurface != null) {
            targetY = lavaSurface - 2.0D + Math.sin(age * 0.035D) * 0.28D;
            this.baseY = targetY;
        } else {
            targetY = this.baseY + Math.sin(age * 0.018D) * 1.6D;
        }

        float yawDelta = Mth.wrapDegrees(this.targetYaw - this.travelYaw);
        this.travelYaw += Mth.clamp(yawDelta, -MAX_TURN_STEP, MAX_TURN_STEP) * TURN_FOLLOW;

        Vec3 forward = new Vec3(-Mth.sin(this.travelYaw * Mth.DEG_TO_RAD), 0,
                Mth.cos(this.travelYaw * Mth.DEG_TO_RAD)).normalize();
        double nextY = this.getY() + Mth.clamp(targetY - this.getY(), -VERTICAL_STEP, VERTICAL_STEP);
        double speed = MOVE_SPEED * (excursionActive ? 1.35D : 1.0D);
        Vec3 nextPosition = this.position().add(forward.scale(speed));
        nextPosition = new Vec3(nextPosition.x, nextY, nextPosition.z);

        Vec3 actualMotion = nextPosition.subtract(this.position());
        if (actualMotion.horizontalDistanceSqr() > 1.0E-7D) {
            this.travelYaw = (float) (Mth.atan2(-actualMotion.x, actualMotion.z) * Mth.RAD_TO_DEG);
        }
        float pitch = (float) (-Mth.atan2(actualMotion.y,
                Math.max(1.0E-4D, actualMotion.horizontalDistance())) * Mth.RAD_TO_DEG);

        this.setYRot(this.travelYaw);
        this.setXRot(Mth.clamp(pitch, -28.0F, 28.0F));
        // Keep the previous orientation for the client's normal entity interpolation.
        this.yRotO = previousYaw;
        this.xRotO = previousPitch;
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

    /** Returns the point at a distance behind the head along its recorded route. */
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

    /** Computes a local banking angle from the change in route tangent around this segment. */
    public float getRollAtDistance(double distance) {
        double sampleOffset = Math.max(0.75D, SEGMENT_SPACING * 0.25D);
        float before = this.getYawAtDistance(Math.max(0.0D, distance - sampleOffset));
        float after = this.getYawAtDistance(distance + sampleOffset);
        float bendRadians = Mth.wrapDegrees(after - before) * Mth.DEG_TO_RAD;
        return Mth.clamp(bendRadians * 0.65F, -0.28F, 0.28F);
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
        for (Vec3 point : this.movementHistory) {
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
