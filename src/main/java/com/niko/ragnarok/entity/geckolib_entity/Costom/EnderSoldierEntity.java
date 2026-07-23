package com.niko.ragnarok.entity.geckolib_entity.Costom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

/**
 * エンダーソルジャー - 8種類の攻撃を持つ強力な敵
 */
public class EnderSoldierEntity extends Monster implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // データシンク
    private static final EntityDataAccessor<Integer> ATTACK_STATE =
            SynchedEntityData.defineId(EnderSoldierEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_DYING =
            SynchedEntityData.defineId(EnderSoldierEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_STUNNED =
            SynchedEntityData.defineId(EnderSoldierEntity.class, EntityDataSerializers.BOOLEAN);

    // 内部状態
    private int customDeathTime = 0;
    private static final int DEATH_DURATION = 39; // 1.97秒
    private int previousAttackState = 0; // アニメーション再生トリガー用

    public EnderSoldierEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 20;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SoldierAttackGoal(this, 1.2D));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 200.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 15.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 2.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ATTACK_STATE, 0);
        this.entityData.define(IS_DYING, false);
        this.entityData.define(IS_STUNNED, false);
    }

    public void setAttackState(int state) {
        this.entityData.set(ATTACK_STATE, state);
    }

    public int getAttackState() {
        return this.entityData.get(ATTACK_STATE);
    }

    public boolean isActuallyDying() {
        return this.entityData.get(IS_DYING);
    }

    private void setDying(boolean dying) {
        this.entityData.set(IS_DYING, dying);
    }

    public boolean isStunned() {
        return this.entityData.get(IS_STUNNED);
    }

    public void setStunned(boolean stunned) {
        this.entityData.set(IS_STUNNED, stunned);
    }

    @Override
    public void die(DamageSource damageSource) {
        if (!this.level().isClientSide && !this.isActuallyDying()) {
            super.die(damageSource);
            this.setDying(true);
            this.customDeathTime = 0;
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.isActuallyDying()) {
            this.customDeathTime++;
            this.setDeltaMovement(Vec3.ZERO);

            // アニメーションが終了する直前（あるいは終了時）にドロップを実行
            if (this.customDeathTime >= DEATH_DURATION) {
                if (!this.level().isClientSide) {
                    // ここでドロップアイテムを放出する
                    this.dropFromLootTable(this.damageSources().generic(), true);

                    this.remove(RemovalReason.KILLED);
                }
            }
        }
    }

    @Override
    protected void tickDeath() {
        // バニラの死亡処理を無効化
    }

    @Override
    public boolean shouldDropExperience() {
        return !this.isBaby();
    }

    @Override
    public boolean isDeadOrDying() {
        return this.isActuallyDying() || super.isDeadOrDying();
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundEvents.ENDERMAN_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return SoundEvents.ENDERMAN_DEATH;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

        // 移動・待機
        controllers.add(new AnimationController<>(this, "base_controller", 5, state -> {

            // 死亡
            if (this.isActuallyDying()) {
                return PlayState.STOP;
            }

            // 攻撃中は完全停止
            if (this.getAttackState() > 0) {
                return state.setAndContinue(
                        RawAnimation.begin().thenLoop("idle")
                );
            }

            // スタン
            if (this.isStunned()) {
                return state.setAndContinue(
                        RawAnimation.begin().thenLoop("idle")
                );
            }

            // 歩行
            if (state.isMoving()) {
                return state.setAndContinue(
                        RawAnimation.begin().thenLoop("walk")
                );
            }

            // 待機
            return state.setAndContinue(
                    RawAnimation.begin().thenLoop("idle")
            );
        }));


        // 攻撃・死亡
        controllers.add(new AnimationController<>(this, "action_controller", 5, state -> {

            // 死亡
            if (this.isActuallyDying()) {

                return state.setAndContinue(
                        RawAnimation.begin().thenPlayAndHold("death")
                );
            }

            // スタン
            if (this.isStunned()) {

                return state.setAndContinue(
                        RawAnimation.begin().thenLoop("idle")
                );
            }

            int attackId = this.getAttackState();

            // 攻撃
            if (attackId > 0) {

                // 攻撃切替時だけリセット
                if (this.previousAttackState != attackId) {

                    state.getController().forceAnimationReset();

                    this.previousAttackState = attackId;
                }

                String animName = attackId == 6
                        ? "superattack"
                        : "attack" + attackId;

                return state.setAndContinue(
                        RawAnimation.begin().thenPlay(animName)
                );
            }

            // 攻撃終了時
            if (this.previousAttackState != 0) {

                this.previousAttackState = 0;

                state.getController().forceAnimationReset();
            }

            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // テレポート処理
    private boolean tryTeleport(double x, double y, double z) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos(x, y, z);

        // 地面まで調整
        while (mutablePos.getY() < this.level().getMaxBuildHeight() && this.level().getBlockState(mutablePos).isSolid()) {
            mutablePos.move(net.minecraft.core.Direction.UP);
        }

        while (mutablePos.getY() > this.level().getMinBuildHeight() && !this.level().getBlockState(mutablePos.below()).isSolid()) {
            mutablePos.move(net.minecraft.core.Direction.DOWN);
        }

        BlockPos targetFloor = mutablePos.immutable();
        AABB targetBox = this.getBoundingBox().move(
                targetFloor.getX() + 0.5D - this.getX(),
                targetFloor.getY() - this.getY(),
                targetFloor.getZ() + 0.5D - this.getZ()
        ).deflate(0.1D);

        if (!this.level().noCollision(this, targetBox) || !this.level().getFluidState(targetFloor).isEmpty()) {
            return false;
        }

        this.teleportTo(targetFloor.getX() + 0.5D, targetFloor.getY(), targetFloor.getZ() + 0.5D);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, this.getSoundSource(), 1.0F, 1.0F);
        spawnTeleportParticles(this.getX(), this.getY(), this.getZ());

        return true;
    }

    private void spawnTeleportParticles(double x, double y, double z) {
        for (int i = 0; i < 64; ++i) {
            double offsetX = (this.random.nextDouble() - 0.5D) * 3.0D;
            double offsetY = this.random.nextDouble() * 3.0D;
            double offsetZ = (this.random.nextDouble() - 0.5D) * 3.0D;

            this.level().addParticle(ParticleTypes.PORTAL,
                    x + offsetX, y + offsetY, z + offsetZ,
                    (this.random.nextDouble() - 0.5D) * 2.0D,
                    -this.random.nextDouble(),
                    (this.random.nextDouble() - 0.5D) * 2.0D);
        }
    }

    // 着地パーティクル
    private void spawnLandingParticles() {
        if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            double x = this.getX();
            double y = this.getY() + 0.1D;
            double z = this.getZ();

            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 20, 1.5D, 0.2D, 1.5D, 0.05D);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);

            for (int i = 0; i < 360; i += 15) {
                double rad = Math.toRadians(i);
                double motionX = Math.cos(rad) * 0.2D;
                double motionZ = Math.sin(rad) * 0.2D;
                serverLevel.sendParticles(ParticleTypes.PORTAL, x, y + 0.5D, z, 0, motionX, 0.1D, motionZ, 1.0D);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);

        // 20%でテレポート回避
        if (!this.level().isClientSide && this.random.nextInt(5) == 0 && !this.isActuallyDying()) {
            double x = this.getX() + (this.random.nextDouble() - 0.5D) * 16.0D;
            double y = this.getY() + (double)(this.random.nextInt(8) - 4);
            double z = this.getZ() + (this.random.nextDouble() - 0.5D) * 16.0D;
            this.tryTeleport(x, y, z);
        }

        return result;
    }

    /**
     * エンダーソルジャー専用攻撃AI
     */
    public static class SoldierAttackGoal extends Goal {

        private final EnderSoldierEntity mob;
        private final double speedModifier;

        private int attackTimer;
        private int cooldownTimer;
        private int stunTimer;
        private Vec3 chargeDirection;

        private int consecutiveAttackCount;

        private LivingEntity lockedTarget;

        public SoldierAttackGoal(EnderSoldierEntity mob, double speedModifier) {

            this.mob = mob;
            this.speedModifier = speedModifier;

            this.setFlags(EnumSet.of(
                    Goal.Flag.MOVE,
                    Goal.Flag.LOOK
            ));
        }


        @Override
        public boolean canUse() {

            if (this.mob.isActuallyDying() || this.mob.isStunned()) {
                return false;
            }

            LivingEntity target = this.mob.getTarget();

            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {

            return (this.mob.getTarget() != null
                    && this.mob.getTarget().isAlive())
                    || this.mob.getAttackState() > 0
                    || this.mob.isStunned();
        }

        @Override
        public void start() {

            this.attackTimer = 0;
            this.cooldownTimer = 0;
            this.stunTimer = 0;

            this.consecutiveAttackCount = 0;
        }

        @Override
        public void stop() {

            this.mob.getNavigation().stop();

            this.mob.setAttackState(0);
            this.mob.setStunned(false);

            this.consecutiveAttackCount = 0;
        }

        @Override
        public void tick() {

            LivingEntity target =
                    this.mob.getAttackState() > 0
                            ? this.lockedTarget
                            : this.mob.getTarget();

            if (this.mob.isActuallyDying()) {
                return;
            }

            // 常にターゲットを見る
            if (target != null) {

                this.mob.getLookControl().setLookAt(
                        target,
                        30.0F,
                        30.0F
                );

                Vec3 lookVec = target.position()
                        .subtract(this.mob.position());

                float yRot = (float)(
                        Math.toDegrees(
                                Math.atan2(lookVec.z, lookVec.x)
                        ) - 90F
                );

                this.mob.setYRot(yRot);
                this.mob.yBodyRot = yRot;
                this.mob.yHeadRot = yRot;
            }

            // 向き固定
            if (target != null) {

                Vec3 lookVec = target.position()
                        .subtract(this.mob.position());

                float yRot = (float) (
                        Math.toDegrees(
                                Math.atan2(lookVec.z, lookVec.x)
                        ) - 90F
                );

                this.mob.setYRot(yRot);
                this.mob.yBodyRot = yRot;
                this.mob.yHeadRot = yRot;
            }

            // スタン
            if (this.mob.isStunned()) {

                stunTimer++;

                this.mob.setDeltaMovement(Vec3.ZERO);

                if (stunTimer >= 60) {

                    this.mob.setStunned(false);

                    this.stunTimer = 0;
                    this.consecutiveAttackCount = 0;
                }

                return;
            }

            // クールダウン
            if (this.cooldownTimer > 0) {

                this.cooldownTimer--;

                this.mob.getNavigation().moveTo(
                        target,
                        this.speedModifier
                );

                return;
            }

            // 攻撃していない時
            if (this.mob.getAttackState() == 0) {

                double distanceSq =
                        this.mob.distanceToSqr(target);

                // 近距離
                if (distanceSq <= 16.0D) {

                    this.mob.getNavigation().stop();

                    Vec3 dash = target.position()
                            .subtract(this.mob.position())
                            .normalize()
                            .scale(0.45D);

                    this.mob.setDeltaMovement(
                            dash.x,
                            this.mob.getDeltaMovement().y,
                            dash.z
                    );

                    this.attackTimer = 0;

                    int attackType =
                            selectAttackType(distanceSq);

                    this.mob.setAttackState(attackType);

                    this.lockedTarget = target;

                    this.mob.playSound(
                            SoundEvents.ENDERMAN_SCREAM,
                            1.0F,
                            1.0F
                    );

                    // 空中急襲準備
                    if (attackType == 7) {

                        this.chargeDirection =
                                target.position()
                                        .subtract(this.mob.position())
                                        .normalize();
                    }

                    // 連撃カウント
                    if (attackType >= 1 && attackType <= 5) {

                        this.consecutiveAttackCount++;
                    }

                } else {

                    // 接近
                    this.mob.getNavigation().moveTo(
                            target,
                            this.speedModifier
                    );
                }

            } else {

                executeAttack();
            }
        }

        private int selectAttackType(double distanceSq) {

            float rand =
                    this.mob.getRandom().nextFloat();

            if (distanceSq > 36.0D && rand < 0.25F) {

                return 6;
            }

            // attack8はanimationに存在しないなら無効化
            if (this.consecutiveAttackCount >= 5) {

                this.consecutiveAttackCount = 0;
            }

            if (rand < 0.22F) {
                return 1;
            } else if (rand < 0.42F) {
                return 2;
            } else if (rand < 0.60F) {
                return 3;
            } else if (rand < 0.76F) {
                return 4;
            } else if (rand < 0.92F) {
                return 5;
            }

            return 6;
        }

        private void executeAttack() {

            LivingEntity target = this.lockedTarget;

            this.attackTimer++;

            this.mob.getNavigation().stop();

            int attackId =
                    this.mob.getAttackState();

            switch (attackId) {

                // attack1
                case 1:

                    if (this.attackTimer == 9) {

                        if (target != null) {
                            target.invulnerableTime = 0;

                            performSingleDamage(
                                    target,
                                    1.0f
                            );
                        }
                    }

                    // 1.25秒 = 25tick
                    if (this.attackTimer >= 15) {

                        finishAttack(8);
                    }

                    break;

// attack2
                case 2:

                    if (this.attackTimer == 7) {

                        if (target != null) {
                            target.invulnerableTime = 0;

                            performSingleDamage(
                                    target,
                                    1.1f
                            );
                        }
                    }

                    if (this.attackTimer >= 15) {

                        finishAttack(8);
                    }

                    break;

// attack3
                case 3:

                    if (this.attackTimer == 9
                            || this.attackTimer == 16) {

                        if (target != null) {

                            target.invulnerableTime = 0;

                            performSingleDamage(
                                    target,
                                    1.2f
                            );
                        }
                    }

                    // 2.06秒 ≒ 41tick
                    if (this.attackTimer >= 28) {

                        finishAttack(11);
                    }

                    break;

// attack4
                case 4:

                    if (this.attackTimer >= 13
                            && this.attackTimer <= 17) {

                        performAreaDamage(
                                4.0D,
                                1.5f
                        );
                    }

                    // 2.45秒 = 49tick
                    if (this.attackTimer >= 40) {

                        finishAttack(13);
                    }

                    break;

// attack5
                case 5:

                    if (this.attackTimer >= 10
                            && this.attackTimer <= 15) {

                        performAreaDamage(
                                4.5D,
                                1.0f
                        );

                        spawnRotationParticles();
                    }

                    // 1.75秒 = 35tick
                    if (this.attackTimer >= 28) {

                        finishAttack(11);
                    }

                    break;
// 上空急襲
                case 6:

                    if (this.attackTimer == 1) {

                        if (target != null) {

                            double targetX = target.getX();
                            double targetY = target.getY() + 10.0D;
                            double targetZ = target.getZ();

                            this.mob.teleportTo(
                                    targetX,
                                    targetY,
                                    targetZ
                            );

                            this.mob.spawnTeleportParticles(
                                    targetX,
                                    targetY,
                                    targetZ
                            );
                        }

                        this.mob.playSound(
                                SoundEvents.ENDERMAN_TELEPORT,
                                1.0F,
                                1.0F
                        );

                        this.chargeDirection =
                                new Vec3(0, -1, 0);
                    }

                    // 突進下降
                    if (this.attackTimer >= 5
                            && this.attackTimer <= 14) {

                        this.mob.setDeltaMovement(
                                this.chargeDirection.scale(1.8D)
                        );

                        performAreaDamage(
                                3.5D,
                                2.5f
                        );

                        // 着地
                        if (this.mob.onGround()) {

                            this.mob.spawnLandingParticles();

                            this.mob.playSound(
                                    SoundEvents.GENERIC_EXPLODE,
                                    1.5F,
                                    0.7F
                            );

                            finishAttack(40);
                            return;
                        }
                    }

                    // 保険
                    if (this.attackTimer >= 30) {

                        finishAttack(40);
                    }

                    break;
            }
        }

        private void performSingleDamage(
                LivingEntity target,
                float multiplier
        ) {

            if (this.mob.distanceToSqr(target)
                    <= 36.0D) {

                float baseDamage =
                        (float) this.mob.getAttributeValue(
                                Attributes.ATTACK_DAMAGE
                        );

                target.hurt(
                        this.mob.damageSources()
                                .mobAttack(this.mob),
                        baseDamage * multiplier
                );

                this.mob.playSound(
                        SoundEvents.PLAYER_ATTACK_STRONG,
                        1.0F,
                        1.0F
                );
            }
        }

        private void performAreaDamage(
                double radius,
                float multiplier
        ) {

            Vec3 look =
                    this.mob.getLookAngle();

            AABB areaBox =
                    this.mob.getBoundingBox()
                            .move(
                                    look.x * radius * 0.7D,
                                    0,
                                    look.z * radius * 0.7D
                            )
                            .inflate(
                                    radius,
                                    2.0D,
                                    radius
                            );

            float baseDamage =
                    (float) this.mob.getAttributeValue(
                            Attributes.ATTACK_DAMAGE
                    );

            for (LivingEntity entity :
                    this.mob.level()
                            .getEntitiesOfClass(
                                    LivingEntity.class,
                                    areaBox
                            )) {

                if (entity != this.mob
                        && entity.isAlive()
                        && entity instanceof LivingEntity
                        && !(entity instanceof EnderSoldierEntity)) {

                    entity.invulnerableTime = 0;

                    entity.hurt(
                            this.mob.damageSources()
                                    .mobAttack(this.mob),
                            baseDamage * multiplier
                    );
                }
            }

            this.mob.playSound(
                    SoundEvents.PLAYER_ATTACK_SWEEP,
                    1.0F,
                    0.8F
            );
        }

        private void spawnRotationParticles() {

            if (this.mob.level().isClientSide) {

                for (int i = 0; i < 8; i++) {

                    double angle =
                            Math.toRadians(
                                    i * 45
                            );

                    double radius = 1.8D;

                    double x =
                            this.mob.getX()
                                    + Math.cos(angle) * radius;

                    double z =
                            this.mob.getZ()
                                    + Math.sin(angle) * radius;

                    this.mob.level().addParticle(
                            ParticleTypes.PORTAL,
                            x,
                            this.mob.getY() + 1.0D,
                            z,
                            0.0D,
                            0.0D,
                            0.0D
                    );
                }
            }
        }

        // 攻撃終了処理
        private void finishAttack(int cooldown) {

            this.attackTimer = 0;

            this.cooldownTimer = cooldown;

            this.mob.setAttackState(0);

            this.mob.setDeltaMovement(Vec3.ZERO);

            this.mob.getNavigation().stop();

            this.lockedTarget = null;
        }
    }
}