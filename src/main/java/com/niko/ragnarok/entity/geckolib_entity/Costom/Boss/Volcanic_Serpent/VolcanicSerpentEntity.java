package com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.Volcanic_Serpent;

import com.niko.ragnarok.entity.Boss_Monster;
import com.niko.ragnarok.entity.RagnarokEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class VolcanicSerpentEntity extends Boss_Monster implements GeoEntity {

    // 胴体12個 + 尻尾1個
    public static final int BODY_SEGMENT_COUNT = 12;
    public static final int TOTAL_PART_COUNT = BODY_SEGMENT_COUNT + 1;

    // 既存モデルの寸法を基準にした初期値
    public static final double HEAD_REAR_JOINT_OFFSET = 23.0D / 16.0D;
    // 頭部から最初の胴体の中心まで
    public static final double HEAD_TO_FIRST_BODY =
            65.0D / 16.0D;
    // 胴体1個分の長さ
    public static final double BODY_SEGMENT_LENGTH =
            84.0D / 16.0D;
    // 最後の胴体中心から尻尾中心まで
    public static final double LAST_BODY_TO_TAIL =
            118.0D / 16.0D;

    private static final EntityDataAccessor<Optional<UUID>> FIRST_PART_UUID =
            SynchedEntityData.defineId(
                    VolcanicSerpentEntity.class,
                    EntityDataSerializers.OPTIONAL_UUID
            );

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);
    private boolean usingSwimNavigation;

    public VolcanicSerpentEntity(
            EntityType<? extends VolcanicSerpentEntity> type,
            Level level
    ) {
        super(type, level);

        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.LAVA, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.BREACH, 0.0F);

        this.setNoGravity(true);
        this.xpReward = 250;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 600.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 18.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(FIRST_PART_UUID, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(
                2,
                new FluidWanderGoal(this)
        );

        this.goalSelector.addGoal(
                3,
                new LookAtPlayerGoal(this, Player.class, 12.0F)
        );

        this.goalSelector.addGoal(
                4,
                new RandomLookAroundGoal(this)
        );

        this.targetSelector.addGoal(
                1,
                new HurtByTargetGoal(this)
        );

        this.targetSelector.addGoal(
                2,
                new NearestAttackableTargetGoal<>(
                        this,
                        Player.class,
                        true
                )
        );
    }

    @Override
    public void tick() {
        super.tick();

        this.updateNavigationMode();

        // パーツ生成はサーバー側だけで行う
        if (!this.level().isClientSide) {
            this.ensurePartsExist();
        }
    }

    private void updateNavigationMode() {
        boolean shouldSwim = this.isInWater() || this.isInLava() || !this.onGround();
        if (shouldSwim == this.usingSwimNavigation) {
            return;
        }

        this.usingSwimNavigation = shouldSwim;
        this.navigation.stop();
        if (shouldSwim) {
            this.navigation = new FlyingPathNavigation(this, this.level());
            this.moveControl = new SwimMoveControl(this);
        } else {
            this.navigation = new GroundPathNavigation(this, this.level());
            this.moveControl = new MoveControl(this);
        }
    }

    /**
     * 初回だけ胴体・尻尾を生成する。
     */
    private void ensurePartsExist() {
        if (this.getFirstPart() != null) {
            return;
        }

        this.setFirstPartUUID(null);

        LivingEntity parent = this;

        for (int i = 0; i < TOTAL_PART_COUNT; i++) {

            VolcanicSerpentPartEntity part =
                    new VolcanicSerpentPartEntity(
                            RagnarokEntities.VOLCANIC_SERPENT_PART.get(),
                            this.level()
                    );

            part.setParent(parent);
            part.setBodyIndex(i);
            part.setTail(i == BODY_SEGMENT_COUNT);

            // 最初の胴体は頭部の後端から、
            // それ以降は直前のパーツから一定距離を取る
            double followDistance;

            if (i == 0) {
                // 頭部から最初の胴体
                followDistance = HEAD_TO_FIRST_BODY;
            } else if (i == BODY_SEGMENT_COUNT) {
                // 最後の胴体から尻尾
                followDistance = LAST_BODY_TO_TAIL;
            } else {
                // 胴体同士
                followDistance = BODY_SEGMENT_LENGTH;
            }

            part.setFollowDistance((float) followDistance);
            part.setInitialPartPos(parent);

            if (!this.level().addFreshEntity(part)) {
                break;
            }

            if (i == 0) {
                this.setFirstPartUUID(part.getUUID());
            }

            // 次のパーツは今回生成したパーツを親にする
            parent = part;
        }
    }

    @Nullable
    public UUID getFirstPartUUID() {
        return this.entityData.get(FIRST_PART_UUID).orElse(null);
    }

    public void setFirstPartUUID(@Nullable UUID uuid) {
        this.entityData.set(
                FIRST_PART_UUID,
                Optional.ofNullable(uuid)
        );
    }

    @Nullable
    public VolcanicSerpentPartEntity getFirstPart() {
        UUID uuid = this.getFirstPartUUID();

        if (uuid == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        if (serverLevel.getEntity(uuid) instanceof VolcanicSerpentPartEntity part) {
            return part;
        }

        return null;
    }

    @Override
    public void travel(Vec3 travelVector) {
        boolean inLiquid = this.isInLava() || this.isInWater();

        if (this.isEffectiveAi() && inLiquid) {
            // Bone Serpentを参考にした液体内移動
            this.moveRelative(this.getSpeed(), travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());

            this.setDeltaMovement(
                    this.getDeltaMovement().scale(0.9D)
            );

            if (this.getTarget() == null) {
                this.setDeltaMovement(
                        this.getDeltaMovement().add(0.0D, -0.005D, 0.0D)
                );
            }
        } else {
            super.travel(travelVector);
        }
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        if (level.getFluidState(pos).is(FluidTags.LAVA)
                || level.getFluidState(pos).is(FluidTags.WATER)) {
            return 10.0F;
        }

        return this.isInLava() ? Float.NEGATIVE_INFINITY : 0.0F;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEAD;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, amount);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.is(DamageTypes.IN_WALL)
                || source.is(DamageTypes.FALL)
                || super.isInvulnerableTo(source);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // アニメーションはRenderer接続時に追加する
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    private static class SwimMoveControl extends MoveControl {
        private final VolcanicSerpentEntity serpent;

        private SwimMoveControl(VolcanicSerpentEntity serpent) {
            super(serpent);
            this.serpent = serpent;
        }

        @Override
        public void tick() {
            if (this.serpent.isInWater() || this.serpent.isInLava()) {
                this.serpent.setDeltaMovement(
                        this.serpent.getDeltaMovement().add(0.0D, 0.005D, 0.0D)
                );
            }

            if (this.operation != Operation.MOVE_TO || this.serpent.getNavigation().isDone()) {
                this.serpent.setSpeed(0.0F);
                this.serpent.setXxa(0.0F);
                this.serpent.setYya(0.0F);
                this.serpent.setZza(0.0F);
                return;
            }

            double dx = this.wantedX - this.serpent.getX();
            double dy = this.wantedY - this.serpent.getY();
            double dz = this.wantedZ - this.serpent.getZ();
            double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

            if (dx * dx + dy * dy + dz * dz < 1.0E-6D) {
                this.serpent.setSpeed(0.0F);
                this.serpent.setZza(0.0F);
                this.serpent.setYya(0.0F);
                return;
            }

            float targetYaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
            float targetPitch = (float) (-Mth.atan2(dy, horizontalDistance) * Mth.RAD_TO_DEG);
            targetPitch = Mth.clamp(Mth.wrapDegrees(targetPitch), -65.0F, 65.0F);

            float[] clearHeading = this.findClearHeading(targetYaw, targetPitch);
            targetYaw = clearHeading[0];
            targetPitch = clearHeading[1];

            float bodyYaw = this.serpent.getYRot();
            float neckYaw = Mth.wrapDegrees(targetYaw - bodyYaw);
            if (Math.abs(neckYaw) > 45.0F) {
                float bodyTargetYaw = targetYaw - Math.copySign(45.0F, neckYaw);
                bodyYaw = this.rotlerp(bodyYaw, bodyTargetYaw, 4.0F);
            }
            this.serpent.setYRot(bodyYaw);
            this.serpent.yBodyRot = bodyYaw;
            this.serpent.setYHeadRot(bodyYaw + Mth.clamp(
                    Mth.wrapDegrees(targetYaw - bodyYaw),
                    -45.0F,
                    45.0F
            ));

            this.serpent.setXRot(this.rotlerp(this.serpent.getXRot(), targetPitch, 5.0F));

            float movementSpeed = (float) (
                    this.speedModifier
                            * this.serpent.getAttributeValue(Attributes.MOVEMENT_SPEED)
                            * 0.2D
            );
            this.serpent.setSpeed(movementSpeed);
            this.serpent.zza = Mth.cos(this.serpent.getXRot() * Mth.DEG_TO_RAD);
            this.serpent.yya = -Mth.sin(this.serpent.getXRot() * Mth.DEG_TO_RAD);
        }

        private float[] findClearHeading(float yaw, float pitch) {
            float[] yawOffsets = {0.0F, 35.0F, -35.0F, 70.0F, -70.0F, 110.0F, -110.0F, 180.0F};
            float[] pitchOffsets = {0.0F, -30.0F, 30.0F};

            for (float pitchOffset : pitchOffsets) {
                float candidatePitch = Mth.clamp(pitch + pitchOffset, -65.0F, 65.0F);
                for (float yawOffset : yawOffsets) {
                    float candidateYaw = Mth.wrapDegrees(yaw + yawOffset);
                    double yawRadians = candidateYaw * Mth.DEG_TO_RAD;
                    double pitchRadians = candidatePitch * Mth.DEG_TO_RAD;
                    Vec3 step = new Vec3(
                            -Mth.sin((float) yawRadians) * Mth.cos((float) pitchRadians),
                            -Mth.sin((float) pitchRadians),
                            Mth.cos((float) yawRadians) * Mth.cos((float) pitchRadians)
                    ).scale(0.8D);

                    if (this.serpent.level().noCollision(
                            this.serpent,
                            this.serpent.getBoundingBox().move(step)
                    )) {
                        return new float[]{candidateYaw, candidatePitch};
                    }
                }
            }

            return new float[]{yaw, pitch};
        }
    }

    private static class FluidWanderGoal extends Goal {
        private final VolcanicSerpentEntity serpent;
        @Nullable
        private BlockPos target;

        private FluidWanderGoal(VolcanicSerpentEntity serpent) {
            this.serpent = serpent;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.serpent.getTarget() != null || this.serpent.getRandom().nextInt(20) != 0) {
                return false;
            }

            this.target = this.serpent.isInWater() || this.serpent.isInLava()
                    ? this.findNearbyFluid()
                    : this.findNearestFluid();
            return this.target != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.serpent.getTarget() == null
                    && this.target != null
                    && !this.serpent.getNavigation().isDone();
        }

        @Override
        public void start() {
            if (this.target != null) {
                this.serpent.getNavigation().moveTo(
                        this.target.getX() + 0.5D,
                        this.target.getY() + 0.5D,
                        this.target.getZ() + 0.5D,
                        0.8D
                );
            }
        }

        @Override
        public void stop() {
            this.target = null;
        }

        @Nullable
        private BlockPos findNearbyFluid() {
            BlockPos origin = this.serpent.blockPosition();
            List<BlockPos> candidates = new ArrayList<>();

            for (int x = -8; x <= 8; x++) {
                for (int y = -5; y <= 5; y++) {
                    for (int z = -8; z <= 8; z++) {
                        BlockPos candidate = origin.offset(x, y, z);
                        if (this.isSwimmable(candidate)) {
                            candidates.add(candidate);
                        }
                    }
                }
            }

            return candidates.isEmpty()
                    ? null
                    : candidates.get(this.serpent.getRandom().nextInt(candidates.size()));
        }

        @Nullable
        private BlockPos findNearestFluid() {
            BlockPos origin = this.serpent.blockPosition();
            BlockPos nearest = null;
            double nearestDistance = Double.MAX_VALUE;

            for (int x = -12; x <= 12; x++) {
                for (int y = -6; y <= 6; y++) {
                    for (int z = -12; z <= 12; z++) {
                        BlockPos candidate = origin.offset(x, y, z);
                        double distance = candidate.distSqr(origin);
                        if (distance < nearestDistance && this.isSwimmable(candidate)) {
                            nearest = candidate;
                            nearestDistance = distance;
                        }
                    }
                }
            }

            return nearest;
        }

        private boolean isSwimmable(BlockPos pos) {
            return !this.serpent.level().getBlockState(pos).blocksMotion()
                    && (this.serpent.level().getFluidState(pos).is(FluidTags.WATER)
                    || this.serpent.level().getFluidState(pos).is(FluidTags.LAVA));
        }
    }
}
