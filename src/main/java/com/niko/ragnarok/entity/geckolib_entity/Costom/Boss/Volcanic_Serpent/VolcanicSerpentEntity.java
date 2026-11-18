package com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.Volcanic_Serpent;

import com.niko.ragnarok.entity.Boss_Monster;
import com.niko.ragnarok.entity.RagnarokEntities;
import com.niko.ragnarok.entity.others.VolcanicSerpentPathNavigation;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
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
    public static final double HEAD_TO_FIRST_BODY = 65.0D / 16.0D;
    // 胴体1個分の長さ
    public static final double BODY_SEGMENT_LENGTH = 84.0D / 16.0D;
    // 最後の胴体中心から尻尾中心まで
    public static final double LAST_BODY_TO_TAIL = 118.0D / 16.0D;

    private static final EntityDataAccessor<Optional<UUID>> FIRST_PART_UUID =
            SynchedEntityData.defineId(
                    VolcanicSerpentEntity.class,
                    EntityDataSerializers.OPTIONAL_UUID
            );

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);
    private boolean usingSwimNavigation;
    private boolean partsSpawned;
    private int breachCooldown;
    private int leapCooldown;

    public VolcanicSerpentEntity(
            EntityType<? extends VolcanicSerpentEntity> type,
            Level level
    ) {
        super(type, level);

        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.LAVA, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.BREACH, 4.0F);

        this.navigation = new VolcanicSerpentPathNavigation(this, level);
        this.moveControl = new SwimMoveControl(this);
        this.usingSwimNavigation = true;
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
        this.goalSelector.addGoal(1, new FluidMeleeLeapGoal(this));
        this.goalSelector.addGoal(2, new SerpentMeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(3, new FluidBreachJumpGoal(this));
        this.goalSelector.addGoal(4, new SwimWanderGoal(this));
        this.goalSelector.addGoal(5, new FindFluidGoal(this));
        this.goalSelector.addGoal(6, new LandWanderGoal(this));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(
                2,
                new NearestAttackableTargetGoal<>(this, Player.class, true)
        );
    }

    @Override
    public void tick() {
        super.tick();

        boolean inLiquid = this.isInFluid();
        this.setNoGravity(inLiquid);
        this.updateNavigationMode(inLiquid);

        // 初期スポーン時や水中遊泳時は yBodyRot のズレを防ぐ
        if (inLiquid || this.tickCount <= 2) {
            this.yBodyRot = this.getYRot();
            this.yBodyRotO = this.yRotO;
        }

        if (this.breachCooldown > 0) {
            this.breachCooldown--;
        }
        if (this.leapCooldown > 0) {
            this.leapCooldown--;
        }

// 飛び出し中の空中では速度ベクトルに合わせて上下角を傾ける
        if (!inLiquid && !this.onGround()) {
            Vec3 motion = this.getDeltaMovement();
            double horiz = motion.horizontalDistance();
            if (horiz > 0.05D || Math.abs(motion.y) > 0.05D) {
                float airPitch = (float) (-Mth.atan2(motion.y, horiz) * Mth.RAD_TO_DEG);
                this.setXRot(Mth.rotLerp(0.25F, this.getXRot(), Mth.clamp(airPitch, -75.0F, 75.0F)));
            }
        } else if (!inLiquid && this.onGround()) {
            // 地上歩行時は頭の傾きを水平に戻す
            this.setXRot(Mth.rotLerp(0.25F, this.getXRot(), 0.0F));
        }

        if (!this.level().isClientSide) {
            this.ensurePartsExist();
        }
    }

    public boolean isInFluid() {
        return this.isInWater() || this.isInLava();
    }

    public static boolean isSerpentFluid(LevelReader level, BlockPos pos) {
        FluidState fluid = level.getFluidState(pos);
        return fluid.is(FluidTags.LAVA) || fluid.is(FluidTags.WATER);
    }

    private void updateNavigationMode(boolean inLiquid) {
        if (inLiquid == this.usingSwimNavigation) {
            return;
        }

        this.usingSwimNavigation = inLiquid;
        this.navigation.stop();
        if (inLiquid) {
            this.navigation = new VolcanicSerpentPathNavigation(this, this.level());
            this.moveControl = new SwimMoveControl(this);
        } else {
            this.navigation = new GroundPathNavigation(this, this.level());
            this.moveControl = new MoveControl(this);
        }
    }

    /**
     * 初回だけ胴体・尻尾を生成する（リロード時は既存パーツと再連結して二重生成を防ぐ）
     */
    private void ensurePartsExist() {
        if (this.getFirstPart() != null) {
            this.partsSpawned = true;
            return;
        }

        // ロード直後は別パーツのチャンク読み込みを少し待つ
        if (this.partsSpawned) {
            List<VolcanicSerpentPartEntity> existing = this.level().getEntitiesOfClass(
                    VolcanicSerpentPartEntity.class,
                    this.getBoundingBox().inflate(32.0D),
                    part -> this.getUUID().equals(part.getParentUUID()) && part.getBodyIndex() == 0
            );
            if (!existing.isEmpty()) {
                this.setFirstPartUUID(existing.get(0).getUUID());
                return;
            }
            if (this.tickCount < 60) {
                return;
            }
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

            double followDistance;
            if (i == 0) {
                followDistance = HEAD_TO_FIRST_BODY;
            } else if (i == BODY_SEGMENT_COUNT) {
                followDistance = LAST_BODY_TO_TAIL;
            } else {
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

            parent = part;
        }

        this.partsSpawned = true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("PartsSpawned", this.partsSpawned);
        UUID firstPartUUID = this.getFirstPartUUID();
        if (firstPartUUID != null) {
            tag.putUUID("FirstPartUUID", firstPartUUID);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.partsSpawned = tag.getBoolean("PartsSpawned");
        if (tag.hasUUID("FirstPartUUID")) {
            this.setFirstPartUUID(tag.getUUID("FirstPartUUID"));
            this.partsSpawned = true;
        }
    }
    @Override
    public int getMaxFallDistance() {
        return 12;
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

        if (serverLevel.getEntity(uuid) instanceof VolcanicSerpentPartEntity part && !part.isRemoved()) {
            return part;
        }

        return null;
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isEffectiveAi() && this.isInFluid()) {
            this.moveRelative(this.getSpeed() * 0.22F, travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.84D));
            this.calculateEntityAnimation(false);
        } else {
            super.travel(travelVector);
        }
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        if (isSerpentFluid(level, pos)) {
            return 10.0F;
        }
        return this.isInFluid() ? -1.0F : 0.0F;
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

    /**
     * 3D空間（上昇・降下・旋回）を直接制御するSwimMoveControl
     */
    private static class SwimMoveControl extends MoveControl {
        private final VolcanicSerpentEntity serpent;

        private SwimMoveControl(VolcanicSerpentEntity serpent) {
            super(serpent);
            this.serpent = serpent;
        }

        @Override
        public void tick() {
            if (this.operation != Operation.MOVE_TO) {
                this.serpent.setSpeed(0.0F);
                this.serpent.setXxa(0.0F);
                this.serpent.setYya(0.0F);
                this.serpent.setZza(0.0F);
                return;
            }

            double dx = this.wantedX - this.serpent.getX();
            double dy = this.wantedY - this.serpent.getY();
            double dz = this.wantedZ - this.serpent.getZ();
            double distSqr = dx * dx + dy * dy + dz * dz;

            if (distSqr < 0.25D) {
                this.serpent.setXxa(0.0F);
                this.serpent.setYya(0.0F);
                this.serpent.setZza(0.0F);
                this.operation = Operation.WAIT;
                return;
            }

            double horizontalDist = Math.sqrt(dx * dx + dz * dz);
            double totalDist = Math.sqrt(distSqr);

            float alignFactor = 1.0F;
            if (horizontalDist > 0.2D) {
                float targetYaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
                float yawDiff = Math.abs(Mth.wrapDegrees(targetYaw - this.serpent.getYRot()));
                float newYaw = this.rotlerp(this.serpent.getYRot(), targetYaw, 12.0F);
                this.serpent.setYRot(newYaw);
                this.serpent.yBodyRot = newYaw;
                this.serpent.setYHeadRot(newYaw);

                // 目標と向きが大きくズレている間は減速して小回りし、周回を防ぐ
                alignFactor = Mth.clamp(Mth.cos(yawDiff * Mth.DEG_TO_RAD), 0.15F, 1.0F);
            }

            float targetPitch = (float) (-Mth.atan2(dy, horizontalDist) * Mth.RAD_TO_DEG);
            targetPitch = Mth.clamp(targetPitch, -75.0F, 75.0F);
            this.serpent.setXRot(this.rotlerp(this.serpent.getXRot(), targetPitch, 8.0F));

            float moveSpeed = (float) (this.speedModifier * this.serpent.getAttributeValue(Attributes.MOVEMENT_SPEED));
            this.serpent.setSpeed(moveSpeed);
            this.serpent.setXxa(0.0F);
            this.serpent.setYya((float) (dy / totalDist));
            this.serpent.setZza((float) (horizontalDist / totalDist) * alignFactor);
        }
    }

    /**
     * 液体内の3D遊泳（遠くまで真っすぐ進みつつ上下深度も変化させる）
     */
    private static class SwimWanderGoal extends Goal {
        private final VolcanicSerpentEntity serpent;
        private Vec3 directTarget;
        private int repathTicks;

        private SwimWanderGoal(VolcanicSerpentEntity serpent) {
            this.serpent = serpent;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return this.serpent.getTarget() == null && this.serpent.isInFluid();
        }

        @Override
        public boolean canContinueToUse() {
            return this.serpent.getTarget() == null && this.serpent.isInFluid();
        }

        @Override
        public void start() {
            this.repathTicks = 0;
            this.directTarget = null;
        }

        @Override
        public void stop() {
            this.directTarget = null;
        }

        @Override
        public void tick() {
            boolean needNewTarget = this.repathTicks-- <= 0
                    || (this.directTarget == null && this.serpent.getNavigation().isDone())
                    || (this.directTarget != null && this.serpent.position().distanceToSqr(this.directTarget) < 6.25D);

            if (needNewTarget) {
                Vec3 dest = this.findSwimDestination();
                if (dest != null) {
                    boolean pathStarted = this.serpent.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.0D);
                    this.directTarget = pathStarted ? null : dest;
                    this.repathTicks = 80 + this.serpent.getRandom().nextInt(60);
                } else {
                    this.directTarget = null;
                    this.repathTicks = 15;
                }
            } else if (this.directTarget != null) {
                // パスが組めなかった場合でも目的地へ直接泳ぐ
                this.serpent.getMoveControl().setWantedPosition(
                        this.directTarget.x,
                        this.directTarget.y,
                        this.directTarget.z,
                        1.0D
                );
            }
        }

        @Nullable
        private Vec3 findSwimDestination() {
            RandomSource random = this.serpent.getRandom();
            BlockPos origin = this.serpent.blockPosition();

            boolean nearSurface = !isSerpentFluid(this.serpent.level(), origin.above(2));
            boolean nearFloor = !isSerpentFluid(this.serpent.level(), origin.below(2));
            float baseYawRad = this.serpent.getYRot() * Mth.DEG_TO_RAD;

            for (int attempt = 0; attempt < 20; attempt++) {
                // 進行方向を中心に±110°の広い範囲から8～24ブロック先を選ぶ
                float angle = baseYawRad + (random.nextFloat() * 2.0F - 1.0F) * (110.0F * Mth.DEG_TO_RAD);
                double dist = 8.0D + random.nextDouble() * 16.0D;
                int dx = Mth.floor(-Mth.sin(angle) * dist);
                int dz = Mth.floor(Mth.cos(angle) * dist);
                int dy = random.nextInt(17) - 8;

                if (nearSurface && !nearFloor && dy > 0) {
                    dy = -dy;
                } else if (nearFloor && !nearSurface && dy < 0) {
                    dy = -dy;
                }

                BlockPos candidate = origin.offset(dx, dy, dz);
                if (isSerpentFluid(this.serpent.level(), candidate)
                        && !this.serpent.level().getBlockState(candidate.above()).blocksMotion()) {
                    return Vec3.atBottomCenterOf(candidate);
                }
            }

            return null;
        }
    }

    /**
     * 非戦闘時：液体表面まで上昇して空中へ飛び出す（ボーンサーペントのJump参考）
     */
    private static class FluidBreachJumpGoal extends Goal {
        private final VolcanicSerpentEntity serpent;
        private double surfaceY;
        private double jumpTargetX;
        private double jumpTargetZ;
        private int ascendTicks;
        private boolean launched;

        private FluidBreachJumpGoal(VolcanicSerpentEntity serpent) {
            this.serpent = serpent;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.serpent.getTarget() != null
                    || this.serpent.breachCooldown > 0
                    || !this.serpent.isInFluid()
                    || this.serpent.getRandom().nextInt(100) != 0) {
                return false;
            }

            BlockPos pos = this.serpent.blockPosition();
            for (int up = 1; up <= 8; up++) {
                BlockPos check = pos.above(up);
                if (!isSerpentFluid(this.serpent.level(), check)) {
                    boolean openAir = this.serpent.level().isEmptyBlock(check)
                            && this.serpent.level().isEmptyBlock(check.above(1))
                            && this.serpent.level().isEmptyBlock(check.above(2));
                    if (openAir) {
                        this.surfaceY = check.getY();
                        float yawRad = this.serpent.getYRot() * Mth.DEG_TO_RAD;
                        this.jumpTargetX = this.serpent.getX() - Mth.sin(yawRad) * 6.0D;
                        this.jumpTargetZ = this.serpent.getZ() + Mth.cos(yawRad) * 6.0D;
                        return true;
                    }
                    return false;
                }
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return !this.launched
                    && this.serpent.getTarget() == null
                    && this.serpent.isInFluid()
                    && this.ascendTicks < 50;
        }

        @Override
        public void start() {
            this.ascendTicks = 0;
            this.launched = false;
            this.serpent.getNavigation().stop();
        }

        @Override
        public void tick() {
            this.ascendTicks++;
            this.serpent.getMoveControl().setWantedPosition(
                    this.jumpTargetX,
                    this.surfaceY + 1.5D,
                    this.jumpTargetZ,
                    1.4D
            );

            if (this.serpent.getY() >= this.surfaceY - 1.4D) {
                Vec3 forward = Vec3.directionFromRotation(0.0F, this.serpent.getYRot()).scale(0.55D);
                this.serpent.setDeltaMovement(forward.x, 0.92D, forward.z);
                this.launched = true;
                this.serpent.breachCooldown = 180 + this.serpent.getRandom().nextInt(160);
            }
        }
    }

    /**
     * 戦闘時：ターゲットが液面の外(上)にいる場合、上昇して飛び掛かる
     */
    private static class FluidMeleeLeapGoal extends Goal {
        private final VolcanicSerpentEntity serpent;
        private double surfaceY;
        private int ascendTicks;
        private boolean leaped;

        private FluidMeleeLeapGoal(VolcanicSerpentEntity serpent) {
            this.serpent = serpent;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.serpent.getTarget();
            if (target == null
                    || !target.isAlive()
                    || this.serpent.leapCooldown > 0
                    || !this.serpent.isInFluid()) {
                return false;
            }

            if (target.isInWater() || target.isInLava()) {
                return false;
            }

            double horizDistSqr = this.serpent.distanceToSqr(target.getX(), this.serpent.getY(), target.getZ());
            double dy = target.getY() - this.serpent.getY();
            if (horizDistSqr < 9.0D || horizDistSqr > 400.0D || dy < 0.5D || dy > 14.0D) {
                return false;
            }

            BlockPos pos = this.serpent.blockPosition();
            for (int up = 1; up <= 10; up++) {
                BlockPos check = pos.above(up);
                if (!isSerpentFluid(this.serpent.level(), check)) {
                    if (!this.serpent.level().getBlockState(check).blocksMotion()
                            && !this.serpent.level().getBlockState(check.above()).blocksMotion()) {
                        this.surfaceY = check.getY();
                        return true;
                    }
                    return false;
                }
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.serpent.getTarget();
            return !this.leaped
                    && target != null
                    && target.isAlive()
                    && this.serpent.isInFluid()
                    && this.ascendTicks < 45;
        }

        @Override
        public void start() {
            this.ascendTicks = 0;
            this.leaped = false;
            this.serpent.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = this.serpent.getTarget();
            if (target == null) {
                return;
            }

            this.ascendTicks++;
            this.serpent.getLookControl().setLookAt(target, 30.0F, 30.0F);
            this.serpent.getMoveControl().setWantedPosition(
                    target.getX(),
                    this.surfaceY + 1.0D,
                    target.getZ(),
                    1.45D
            );

            if (this.serpent.getY() >= this.surfaceY - 1.5D) {
                Vec3 dir = target.position().subtract(this.serpent.position());
                double horiz = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
                if (horiz > 1.0E-4D) {
                    double speed = Math.min(1.1D, 0.55D + horiz * 0.04D);
                    double vy = Mth.clamp(0.65D + (target.getY() - this.serpent.getY()) * 0.08D, 0.65D, 1.15D);
                    this.serpent.setDeltaMovement(
                            (dir.x / horiz) * speed,
                            vy,
                            (dir.z / horiz) * speed
                    );
                }
                this.leaped = true;
                this.serpent.leapCooldown = 90;
            }
        }
    }

    /**
     * 液体内3D追尾対応の近接攻撃ゴール
     */
    private static class SerpentMeleeAttackGoal extends MeleeAttackGoal {
        private final VolcanicSerpentEntity serpent;

        private SerpentMeleeAttackGoal(VolcanicSerpentEntity serpent, double speedModifier, boolean followIfNotSeen) {
            super(serpent, speedModifier, followIfNotSeen);
            this.serpent = serpent;
        }

        @Override
        public void tick() {
            super.tick();
            LivingEntity target = this.serpent.getTarget();
            if (target == null || !this.serpent.isInFluid()) {
                return;
            }

            if (this.serpent.getNavigation().isDone() || this.serpent.getNavigation().getPath() == null) {
                this.serpent.getMoveControl().setWantedPosition(
                        target.getX(),
                        target.getY(),
                        target.getZ(),
                        1.25D
                );
            }
        }
    }

    /**
     * 陸に上がった際、近くのマグマや水を探して中まで入る
     */
    private static class FindFluidGoal extends Goal {
        private final VolcanicSerpentEntity serpent;
        private BlockPos targetFluidPos;
        private int searchCooldown;
        private int stuckTicks;
        private double lastDistSqr;

        private FindFluidGoal(VolcanicSerpentEntity serpent) {
            this.serpent = serpent;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.serpent.isInFluid() || this.serpent.getTarget() != null) {
                return false;
            }
            if (this.searchCooldown > 0) {
                this.searchCooldown--;
                return false;
            }
            this.targetFluidPos = this.findNearestFluid();
            this.searchCooldown = 20;
            return this.targetFluidPos != null;
        }

        @Override
        public boolean canContinueToUse() {
            return !this.serpent.isInFluid()
                    && this.serpent.getTarget() == null
                    && this.targetFluidPos != null
                    && isSerpentFluid(this.serpent.level(), this.targetFluidPos)
                    && this.stuckTicks < 80;
        }

        @Override
        public void start() {
            this.stuckTicks = 0;
            this.lastDistSqr = Double.MAX_VALUE;
            if (this.targetFluidPos != null) {
                this.serpent.getNavigation().moveTo(
                        this.targetFluidPos.getX() + 0.5D,
                        this.targetFluidPos.getY() + 0.5D,
                        this.targetFluidPos.getZ() + 0.5D,
                        1.05D
                );
            }
        }

        @Override
        public void stop() {
            if (this.stuckTicks >= 80) {
                // 障害物等で辿り着けない時は一時クールダウンを入れて LandWanderGoal に任せる
                this.searchCooldown = 100;
            }
            this.targetFluidPos = null;
            this.serpent.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (this.targetFluidPos == null) {
                return;
            }

            double tx = this.targetFluidPos.getX() + 0.5D;
            double ty = this.targetFluidPos.getY() + 0.5D;
            double tz = this.targetFluidPos.getZ() + 0.5D;
            double distSqr = this.serpent.distanceToSqr(tx, ty, tz);

            // 液体の近くまたは岸辺でパスが止まった場合、直接液体の中へ踏み込む
            if (distSqr <= 25.0D || this.serpent.getNavigation().isDone()) {
                this.serpent.getMoveControl().setWantedPosition(tx, ty, tz, 1.15D);
                if (this.serpent.horizontalCollision && this.serpent.onGround()) {
                    this.serpent.getJumpControl().jump();
                }
            }

            if (this.serpent.tickCount % 20 == 0) {
                if (distSqr > this.lastDistSqr - 1.0D) {
                    this.stuckTicks += 20;
                } else {
                    this.stuckTicks = 0;
                    this.lastDistSqr = distSqr;
                }
            }
        }

        @Nullable
        private BlockPos findNearestFluid() {
            BlockPos origin = this.serpent.blockPosition();
            BlockPos closest = null;
            double bestDistSqr = Double.MAX_VALUE;

            for (int dx = -24; dx <= 24; dx += 2) {
                for (int dz = -24; dz <= 24; dz += 2) {
                    for (int dy = -10; dy <= 6; dy += 2) {
                        BlockPos pos = origin.offset(dx, dy, dz);
                        if (isSerpentFluid(this.serpent.level(), pos)
                                && !this.serpent.level().getBlockState(pos.above()).blocksMotion()) {
                            double distSqr = pos.distSqr(origin);
                            if (distSqr < bestDistSqr) {
                                bestDistSqr = distSqr;
                                closest = pos;
                            }
                        }
                    }
                }
            }
            return closest;
        }
    }

    /**
     * 地上用ランダム徘徊（近くに液体がない場合でも動き回る）
     */
    private static class LandWanderGoal extends Goal {
        private final VolcanicSerpentEntity serpent;
        private double targetX;
        private double targetY;
        private double targetZ;
        private int moveTicks;

        private LandWanderGoal(VolcanicSerpentEntity serpent) {
            this.serpent = serpent;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.serpent.isInFluid()
                    || this.serpent.getTarget() != null
                    || this.serpent.getRandom().nextInt(30) != 0) {
                return false;
            }

            Vec3 pos = DefaultRandomPos.getPos(this.serpent, 16, 7);
            if (pos == null) {
                pos = LandRandomPos.getPos(this.serpent, 16, 7);
            }
            if (pos == null) {
                return false;
            }

            this.targetX = pos.x;
            this.targetY = pos.y;
            this.targetZ = pos.z;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return !this.serpent.isInFluid()
                    && this.serpent.getTarget() == null
                    && !this.serpent.getNavigation().isDone()
                    && this.moveTicks > 0;
        }

        @Override
        public void start() {
            this.moveTicks = 120;
            this.serpent.getNavigation().moveTo(this.targetX, this.targetY, this.targetZ, 0.85D);
        }

        @Override
        public void tick() {
            this.moveTicks--;
        }

        @Override
        public void stop() {
            this.serpent.getNavigation().stop();
        }
    }
}