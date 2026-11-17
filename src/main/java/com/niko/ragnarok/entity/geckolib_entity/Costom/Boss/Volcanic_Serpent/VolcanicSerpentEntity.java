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
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
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
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

public class VolcanicSerpentEntity extends Boss_Monster implements GeoEntity {

    private static final float MAX_FLUID_WANDER_PITCH = 35.0F;

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
    private boolean fluidWandering;
    private float fluidWanderYaw;
    private float fluidWanderPitch;
    private int fluidWanderTurnTicks;

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
                new WaterAvoidingRandomStrollGoal(this, 0.5D)
        );

        this.goalSelector.addGoal(
                4,
                new LookAtPlayerGoal(this, Player.class, 12.0F)
        );

        this.goalSelector.addGoal(
                5,
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
                    this.getDeltaMovement().scale(0.8D)
            );
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
        private static final double MIN_YAW_UPDATE_DISTANCE_SQR = 0.64D;

        private final VolcanicSerpentEntity serpent;

        private SwimMoveControl(VolcanicSerpentEntity serpent) {
            super(serpent);
            this.serpent = serpent;
        }

        @Override
        public void tick() {
            if (this.serpent.fluidWandering
                    && this.serpent.getTarget() == null
                    && (this.serpent.isInWater() || this.serpent.isInLava())) {
                this.tickFluidWander();
                return;
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
            double horizontalDistanceSqr = dx * dx + dz * dz;
            double horizontalDistance = Math.sqrt(horizontalDistanceSqr);

            if (dx * dx + dy * dy + dz * dz < 1.0E-6D) {
                this.serpent.setSpeed(0.0F);
                this.serpent.setZza(0.0F);
                this.serpent.setYya(0.0F);
                return;
            }

            boolean closeHorizontally = horizontalDistanceSqr < MIN_YAW_UPDATE_DISTANCE_SQR;
            float targetYaw = closeHorizontally
                    ? this.serpent.getYRot()
                    : (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
            float targetPitch = (float) (-Mth.atan2(dy, horizontalDistance) * Mth.RAD_TO_DEG);
            targetPitch = Mth.clamp(Mth.wrapDegrees(targetPitch), -65.0F, 65.0F);

            float bodyYaw = this.serpent.getYRot();
            float neckYaw = Mth.wrapDegrees(targetYaw - bodyYaw);
            if (Math.abs(neckYaw) > 45.0F) {
                float bodyTargetYaw = targetYaw - Math.copySign(45.0F, neckYaw);
                bodyYaw = this.rotlerp(bodyYaw, bodyTargetYaw, 8.0F);
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
                            * 0.08D
            );
            this.serpent.setSpeed(movementSpeed);
            this.serpent.zza = closeHorizontally
                    ? 0.0F
                    : Mth.cos(this.serpent.getXRot() * Mth.DEG_TO_RAD);
            this.serpent.yya = -Mth.sin(this.serpent.getXRot() * Mth.DEG_TO_RAD);
        }

        private void tickFluidWander() {
            if (this.serpent.fluidWanderTurnTicks-- <= 0) {
                this.serpent.chooseFluidWanderHeading(false);
            }

            Vec3 direction = this.getWanderDirection();
            Vec3 nextPosition = this.serpent.position().add(direction);
            BlockPos nextBlock = BlockPos.containing(nextPosition);
            boolean matchingFluid = this.serpent.isInLava()
                    ? this.serpent.level().getFluidState(nextBlock).is(FluidTags.LAVA)
                    : this.serpent.level().getFluidState(nextBlock).is(FluidTags.WATER);
            if (!matchingFluid || !this.serpent.level().noCollision(
                    this.serpent,
                    this.serpent.getBoundingBox().move(direction)
            )) {
                this.serpent.chooseFluidWanderHeading(true);
            }

            this.serpent.setYRot(this.rotlerp(
                    this.serpent.getYRot(),
                    this.serpent.fluidWanderYaw,
                    8.0F
            ));
            this.serpent.yBodyRot = this.serpent.getYRot();
            this.serpent.setYHeadRot(this.serpent.getYRot());
            this.serpent.setXRot(this.rotlerp(
                    this.serpent.getXRot(),
                    this.serpent.fluidWanderPitch,
                    5.0F
            ));
            this.serpent.setSpeed((float) (
                    this.serpent.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.08D
            ));
            this.serpent.setXxa(0.0F);
            this.serpent.zza = Mth.cos(this.serpent.getXRot() * Mth.DEG_TO_RAD);
            this.serpent.yya = -Mth.sin(this.serpent.getXRot() * Mth.DEG_TO_RAD);
        }

        private Vec3 getWanderDirection() {
            float yaw = this.serpent.fluidWanderYaw * Mth.DEG_TO_RAD;
            float pitch = this.serpent.fluidWanderPitch * Mth.DEG_TO_RAD;
            return new Vec3(
                    -Mth.sin(yaw) * Mth.cos(pitch),
                    -Mth.sin(pitch),
                    Mth.cos(yaw) * Mth.cos(pitch)
            ).scale(0.8D);
        }
    }

    private static class FluidWanderGoal extends Goal {
        private final VolcanicSerpentEntity serpent;

        private FluidWanderGoal(VolcanicSerpentEntity serpent) {
            this.serpent = serpent;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.serpent.getTarget() != null
                    || this.serpent.getRandom().nextInt(20) != 0) {
                return false;
            }

            return this.serpent.isInWater() || this.serpent.isInLava();
        }

        @Override
        public boolean canContinueToUse() {
            return this.serpent.getTarget() == null
                    && (this.serpent.isInWater() || this.serpent.isInLava());
        }

        @Override
        public void start() {
            this.serpent.fluidWandering = true;
            this.serpent.fluidWanderYaw = this.serpent.getYRot()
                    + this.serpent.getRandom().nextFloat() * 90.0F - 45.0F;
            this.serpent.fluidWanderPitch = this.serpent.getRandom().nextFloat()
                    * MAX_FLUID_WANDER_PITCH * 2.0F - MAX_FLUID_WANDER_PITCH;
            this.serpent.fluidWanderTurnTicks = 0;
            this.serpent.getNavigation().stop();
        }

        @Override
        public void stop() {
            this.serpent.fluidWandering = false;
            this.serpent.getNavigation().stop();
        }
    }

    private void chooseFluidWanderHeading(boolean avoidObstacle) {
        float turn = avoidObstacle
                ? 90.0F + this.getRandom().nextFloat() * 60.0F
                : 25.0F + this.getRandom().nextFloat() * 50.0F;
        if (this.getRandom().nextBoolean()) {
            turn = -turn;
        }

        this.fluidWanderYaw = Mth.wrapDegrees(this.fluidWanderYaw + turn);
        this.fluidWanderPitch = this.getRandom().nextFloat()
                * MAX_FLUID_WANDER_PITCH * 2.0F - MAX_FLUID_WANDER_PITCH;
        this.fluidWanderTurnTicks = avoidObstacle
                ? 12
                : 80 + this.getRandom().nextInt(81);
    }
}
