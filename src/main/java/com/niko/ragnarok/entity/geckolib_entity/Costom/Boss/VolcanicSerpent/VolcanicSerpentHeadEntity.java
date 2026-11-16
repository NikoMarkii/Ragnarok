package com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.VolcanicSerpent;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.concurrent.atomic.AtomicInteger;

public class VolcanicSerpentHeadEntity extends Monster implements GeoEntity {
    private static final AtomicInteger ENTITY_COUNTER = new AtomicInteger();

    // 体節同期用データ
    private static final EntityDataAccessor<CompoundTag> BODY_POSE = SynchedEntityData.defineId(VolcanicSerpentHeadEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Float> HEAD_ROLL = SynchedEntityData.defineId(VolcanicSerpentHeadEntity.class, EntityDataSerializers.FLOAT);

    // 設定値：体節の総数と間隔（モデルの長さに合わせて調整）
    public static final int SEGMENT_COUNT = 20;
    public static final double SEGMENT_SPACING = 2.6D;

    // 移動・旋回パラメータ
    private static final double SWIM_SPEED = 0.65D;
    private static final double BREACH_SPEED = 0.85D;
    private static final double GRAVITY_ACCEL = 0.045D;
    private static final double MAX_TURN_RADIANS = Math.toRadians(7.5D);

    private final VolcanicSerpentBodyEntity body;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private Vec3 swimVelocity = Vec3.ZERO;
    private int breachCooldown = 80;
    private int breachTicks = 0;
    private boolean isBreaching = false;
    private float prevRoll = 0.0F;
    private float currentRoll = 0.0F;

    public VolcanicSerpentHeadEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.lookControl = new LookControl(this) {
            @Override
            protected boolean resetXRotOnTick() {
                return false;
            }
        };
        this.noPhysics = false;
        // 重力は独自物理演算（液体外のみ適用）で制御するためバニラ重力はオフ
        this.setNoGravity(true);
        this.setMaxUpStep(1.5F);

        this.body = new VolcanicSerpentBodyEntity(this, SEGMENT_COUNT - 1);
        this.setId(ENTITY_COUNTER.getAndAdd(SEGMENT_COUNT) + 1);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 600.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.45D)
                .add(Attributes.ATTACK_DAMAGE, 18.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(BODY_POSE, new CompoundTag());
        this.entityData.define(HEAD_ROLL, 0.0F);
    }

    public VolcanicSerpentBodyEntity getBody() {
        return body;
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public VolcanicSerpentPartEntity[] getParts() {
        return body.getParts();
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        if (body != null) body.assignIds(id);
    }

    @Override
    public void setPos(double x, double y, double z) {
        super.setPos(x, y, z);
        if (body != null) body.initializePosition(SEGMENT_SPACING);
    }

    @Override
    public void onRemovedFromWorld() {
        if (isRemoved()) body.remove();
        super.onRemovedFromWorld();
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    public float getRoll(float partialTick) {
        return prevRoll + Mth.wrapDegrees(currentRoll - prevRoll) * partialTick;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (BODY_POSE.equals(key) && this.level().isClientSide && this.body != null) {
            this.body.readSnapshot(this.entityData.get(BODY_POSE));
        } else if (HEAD_ROLL.equals(key)) {
            this.prevRoll = this.currentRoll;
            this.currentRoll = this.entityData.get(HEAD_ROLL);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            tickSerpentMovement();
            this.body.update(SEGMENT_SPACING);
            if (!this.isRemoved()) {
                this.entityData.set(BODY_POSE, this.body.createSnapshot());
            }
        } else {
            this.body.tickClientInterpolation();
        }
    }

    private boolean isInLiquid() {
        if (this.isInWater() || this.isInLava()) return true;
        FluidState state = this.level().getFluidState(this.blockPosition());
        return state.is(FluidTags.LAVA) || state.is(FluidTags.WATER);
    }

    private void tickSerpentMovement() {
        boolean inLiquid = isInLiquid();
        LivingEntity target = this.getTarget();

        if (this.swimVelocity.lengthSqr() < 1.0E-4) {
            Vec3 look = this.getLookAngle();
            this.swimVelocity = (look.lengthSqr() > 1.0E-4 ? look : new Vec3(0, 0, 1)).normalize().scale(SWIM_SPEED);
        }

        if (inLiquid) {
            // --- 液体内（溶岩・水）：重力なし＆くねくね泳ぎ＆時々頭出し ---
            if (breachCooldown > 0) breachCooldown--;

            // 一定間隔で水面（溶岩面）から頭を出す行動を開始
            if (!isBreaching && breachCooldown <= 0 && this.random.nextInt(40) == 0) {
                isBreaching = true;
                breachTicks = 35;
                breachCooldown = 140 + this.random.nextInt(100);
            }

            Vec3 desiredDir;
            double targetSpeed = SWIM_SPEED;

            if (isBreaching) {
                breachTicks--;
                // ターゲットがいる場合はターゲット方向へ斜め上に飛び出す
                Vec3 toTarget = (target != null && target.isAlive())
                        ? target.position().subtract(this.position()).multiply(1, 0, 1).normalize()
                        : new Vec3(this.swimVelocity.x, 0, this.swimVelocity.z).normalize();
                if (toTarget.lengthSqr() < 1.0E-4) toTarget = new Vec3(1, 0, 0);

                desiredDir = toTarget.scale(0.65D).add(0.0D, 0.85D, 0.0D).normalize();
                targetSpeed = BREACH_SPEED;

                if (breachTicks <= 0) {
                    isBreaching = false;
                }
            } else {
                // 通常の游弋：ターゲット追跡 または 自由游弋
                Vec3 baseDir;
                if (target != null && target.isAlive()) {
                    // 溶岩の少し下に潜りつつ近づくようにY座標を調整
                    Vec3 aimPos = target.position().add(0, -1.5D, 0);
                    baseDir = aimPos.subtract(this.position()).normalize();
                } else {
                    // ターゲットがいない場合も緩やかに旋回しながら泳ぎ続ける
                    float wanderYaw = this.getYRot() + Mth.sin(this.tickCount * 0.05F) * 4.0F;
                    double rad = Math.toRadians(wanderYaw);
                    // 深く潜りすぎず、上に出すぎないようY成分を調整
                    double yBias = isLiquidAbove() ? 0.05D : -0.15D;
                    baseDir = new Vec3(-Math.sin(rad), yBias, Math.cos(rad)).normalize();
                }

                // くねくね泳ぐための横方向のサイン波（蛇行運動）
                Vec3 horizontalForward = new Vec3(baseDir.x, 0, baseDir.z);
                if (horizontalForward.lengthSqr() < 1.0E-4) horizontalForward = new Vec3(0, 0, 1);
                horizontalForward = horizontalForward.normalize();
                Vec3 rightVec = new Vec3(-horizontalForward.z, 0, horizontalForward.x);

                double slitherWave = Math.sin(this.tickCount * 0.22D) * 0.55D;
                double verticalWave = Math.cos(this.tickCount * 0.16D) * 0.18D;

                desiredDir = baseDir.add(rightVec.scale(slitherWave)).add(0, verticalWave, 0).normalize();
            }

            // 急激に折れ曲がらないよう旋回角度を制限して速度を更新
            Vec3 desiredVel = desiredDir.scale(targetSpeed);
            this.swimVelocity = limitTurn(this.swimVelocity, desiredVel, MAX_TURN_RADIANS);

            // くねくねに合わせて頭部を少し傾ける（ロール）
            float targetRoll = Mth.sin(this.tickCount * 0.22F) * 22.0F;
            this.entityData.set(HEAD_ROLL, Mth.lerp(0.2F, this.currentRoll, targetRoll));

        } else {
            // --- 液体外（空中・地上）：重力を適用し、立ち止まらないように前進 ---
            isBreaching = false;

            // 重力落下を加算
            this.swimVelocity = this.swimVelocity.add(0.0D, -GRAVITY_ACCEL, 0.0D);

            // 水平速度が落ちすぎないよう最低速度を維持（立ち止まり防止）
            Vec3 horiz = new Vec3(this.swimVelocity.x, 0, this.swimVelocity.z);
            double minHorizSpeed = this.onGround() ? 0.35D : 0.25D;

            if (horiz.lengthSqr() < minHorizSpeed * minHorizSpeed) {
                Vec3 forward = (target != null && target.isAlive())
                        ? target.position().subtract(this.position()).multiply(1, 0, 1)
                        : Vec3.directionFromRotation(0, this.getYRot());
                if (forward.lengthSqr() < 1.0E-4) forward = new Vec3(1, 0, 0);
                horiz = forward.normalize().scale(minHorizSpeed);
            } else if (this.onGround()) {
                // 地上を這う場合もターゲット方向へくねくねしながら進む
                Vec3 forward = (target != null && target.isAlive())
                        ? target.position().subtract(this.position()).multiply(1, 0, 1).normalize()
                        : horiz.normalize();
                Vec3 right = new Vec3(-forward.z, 0, forward.x);
                Vec3 slither = forward.add(right.scale(Math.sin(this.tickCount * 0.25D) * 0.4D)).normalize().scale(0.4D);
                horiz = limitTurn(horiz, slither, MAX_TURN_RADIANS);
            }

            this.swimVelocity = new Vec3(horiz.x, this.swimVelocity.y, horiz.z);
            this.entityData.set(HEAD_ROLL, Mth.lerp(0.15F, this.currentRoll, 0.0F));
        }

        // 移動実行
        this.setDeltaMovement(this.swimVelocity);
        this.move(MoverType.SELF, this.getDeltaMovement());

        // 衝突時の壁滑り・方向転換（スタックして立ち止まるのを防止）
        Vec3 actualMove = this.getDeltaMovement();
        if (this.horizontalCollision) {
            float turnAngle = (this.tickCount % 60 < 30) ? 40.0F : -40.0F;
            float newYaw = this.getYRot() + turnAngle;
            Vec3 bounceHoriz = Vec3.directionFromRotation(0, newYaw).scale(SWIM_SPEED * 0.8D);
            this.swimVelocity = new Vec3(bounceHoriz.x, this.swimVelocity.y, bounceHoriz.z);
        } else {
            this.swimVelocity = new Vec3(actualMove.x, this.swimVelocity.y, actualMove.z);
        }

        if (this.verticalCollision) {
            if (this.onGround() && !inLiquid) {
                // 地上では少し跳ねるようにして段差や摩擦で止まらないようにする
                this.swimVelocity = new Vec3(this.swimVelocity.x, 0.12D, this.swimVelocity.z);
            } else {
                this.swimVelocity = new Vec3(this.swimVelocity.x, -this.swimVelocity.y * 0.4D, this.swimVelocity.z);
            }
        }

        // 進行方向へ頭部の向き（Yaw / Pitch）を滑らかに合わせる
        if (this.swimVelocity.lengthSqr() > 1.0E-5) {
            double hDist = this.swimVelocity.horizontalDistance();
            if (hDist > 1.0E-4) {
                float targetYaw = (float) (Mth.atan2(-this.swimVelocity.x, this.swimVelocity.z) * Mth.RAD_TO_DEG);
                this.setYRot(Mth.rotLerp(0.25F, this.getYRot(), targetYaw));
                this.yBodyRot = this.getYRot();
                this.yHeadRot = this.getYRot();
            }
            float targetPitch = (float) (-Mth.atan2(this.swimVelocity.y, hDist) * Mth.RAD_TO_DEG);
            this.setXRot(Mth.rotLerp(0.25F, this.getXRot(), targetPitch));
        }
    }

    private boolean isLiquidAbove() {
        BlockPos above = this.blockPosition().above(2);
        FluidState state = this.level().getFluidState(above);
        return state.is(FluidTags.LAVA) || state.is(FluidTags.WATER);
    }

    // デストロイヤーのWormSteeringを参考にした最大旋回角制限
    private static Vec3 limitTurn(Vec3 current, Vec3 desired, double maxAngleRad) {
        double desiredLen = desired.length();
        if (desiredLen < 1.0E-6) return current;
        double currentLen = current.length();
        if (currentLen < 1.0E-6) return desired;

        Vec3 curDir = current.scale(1.0D / currentLen);
        Vec3 desDir = desired.scale(1.0D / desiredLen);

        double dot = Mth.clamp(curDir.dot(desDir), -1.0D, 1.0D);
        double angle = Math.acos(dot);

        Vec3 nextDir;
        if (angle <= maxAngleRad || angle < 1.0E-5) {
            nextDir = desDir;
        } else {
            double t = maxAngleRad / angle;
            double sinAngle = Math.sin(angle);
            double w1 = Math.sin((1.0D - t) * angle) / sinAngle;
            double w2 = Math.sin(t * angle) / sinAngle;
            nextDir = curDir.scale(w1).add(desDir.scale(w2)).normalize();
        }

        double nextSpeed = Mth.lerp(0.2D, currentLen, desiredLen);
        return nextDir.scale(nextSpeed);
    }

    public boolean hurtFromPart(VolcanicSerpentPartEntity part, DamageSource source, float amount) {
        return super.hurt(source, amount);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble("SerpentVX", swimVelocity.x);
        tag.putDouble("SerpentVY", swimVelocity.y);
        tag.putDouble("SerpentVZ", swimVelocity.z);
        body.save(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("SerpentVX")) {
            swimVelocity = new Vec3(tag.getDouble("SerpentVX"), tag.getDouble("SerpentVY"), tag.getDouble("SerpentVZ"));
        }
        body.load(tag, SEGMENT_SPACING);
        this.entityData.set(BODY_POSE, body.createSnapshot());
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}
}