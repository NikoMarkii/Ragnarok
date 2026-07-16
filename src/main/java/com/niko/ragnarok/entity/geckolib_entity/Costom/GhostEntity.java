package com.niko.ragnarok.entity.geckolib_entity.Costom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.List;

public class GhostEntity extends Monster implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Integer> ATTACK_STATE =
            SynchedEntityData.defineId(GhostEntity.class, EntityDataSerializers.INT);

    // 攻撃タイミング定数
    private static final int ATK1_HIT = 10;
    private static final int ATK1_END = 15;
    private static final int ATK2_HIT = 20;
    private static final int ATK2_END = 40;

    public GhostEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.moveControl = new GhostMoveControl(this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH,      40.0D)
                .add(Attributes.MOVEMENT_SPEED,   0.25D)
                .add(Attributes.ATTACK_DAMAGE,     6.0D)
                .add(Attributes.FOLLOW_RANGE,      24.0D)
                .add(Attributes.ARMOR,              4.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ATTACK_STATE, 0);
    }

    public int getAttackState()       { return this.entityData.get(ATTACK_STATE); }
    public void setAttackState(int s) { this.entityData.set(ATTACK_STATE, s);     }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new GhostAttackGoal(this));
        this.goalSelector.addGoal(2, new GhostFloatGoal(this));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 16.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this, Player.class, true));
    }
    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide()) {
            if (this.level().isDay()
                    && !this.level().isRaining()
                    && !this.isOnFire()) {
                float brightness = this.getLightLevelDependentMagicValue();
                BlockPos pos = this.blockPosition();
                if (brightness > 0.5F
                        && this.random.nextFloat() * 30.0F < (brightness - 0.4F) * 2.0F
                        && this.level().canSeeSky(pos)) {
                    this.setSecondsOnFire(8);
                }
            }
        }
    }
    @Override
    public boolean isNoGravity() {
        return true;
    }

    // ブロックをすり抜けるためpushEntities等を無効化
    @Override
    protected void doPush(Entity entity) {}

    @Override
    public boolean isPushable() { return false; }

    @Override
    protected boolean isAffectedByFluids() { return false; }

    @Override
    public boolean causeFallDamage(float d, float m, DamageSource s) { return false; }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {}

    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.AMBIENT_CAVE.get(); }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.SKELETON_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.SKELETON_DEATH; }

    // ──────────────────────────────────────────
    // GhostMoveControl：ブロックを無視して3D移動
    // ──────────────────────────────────────────
    static class GhostMoveControl extends MoveControl {

        private final GhostEntity ghost;
        // 慣性係数（大きいほど滑らか・遅く方向転換）
        private static final double INERTIA     = 0.91;
        // 加速力（小さいほどゆっくり加速）
        private static final double ACCEL       = 0.05;
        // 空中抵抗
        private static final double AIR_RESIST  = 0.91;

        GhostMoveControl(GhostEntity ghost) {
            super(ghost);
            this.ghost = ghost;
        }

        @Override
        public void tick() {
            Vec3 current = ghost.getDeltaMovement();

            if (this.operation == MoveControl.Operation.MOVE_TO) {
                Vec3 toTarget = new Vec3(
                        this.wantedX - ghost.getX(),
                        this.wantedY - ghost.getY(),
                        this.wantedZ - ghost.getZ()
                );
                double dist = toTarget.length();

                if (dist < 0.5) {
                    this.operation = MoveControl.Operation.WAIT;
                    // 到達したら慣性で止まる
                    ghost.setDeltaMovement(current.scale(0.7));
                    return;
                }

                double speed = this.speedModifier
                        * ghost.getAttributeValue(Attributes.MOVEMENT_SPEED);

                // 目標方向への加速度を現在速度に加算（アレイ式）
                Vec3 accel = toTarget.normalize().scale(speed * ACCEL);
                Vec3 newVel = current.add(accel).scale(AIR_RESIST);

                // 最大速度でクランプ
                double maxSpeed = speed * 1.5;
                if (newVel.horizontalDistance() > maxSpeed) {
                    newVel = new Vec3(
                            newVel.x / newVel.horizontalDistance() * maxSpeed,
                            newVel.y,
                            newVel.z / newVel.horizontalDistance() * maxSpeed
                    );
                }

                ghost.setDeltaMovement(newVel);

                // 進行方向に滑らかに向く
                double dx = this.wantedX - ghost.getX();
                double dz = this.wantedZ - ghost.getZ();
                if (Math.abs(dx) > 1e-5 || Math.abs(dz) > 1e-5) {
                    float yaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
                    // rotlerpで滑らかに回転（5.0F = 毎tickの最大回転量）
                    ghost.setYRot(rotlerp(ghost.getYRot(), yaw, 5.0F));
                    ghost.yBodyRot = ghost.getYRot();
                }

            } else {
                // WAIT中は慣性で徐々に停止
                ghost.setDeltaMovement(current.scale(INERTIA));
            }
        }
    }
    public static boolean checkGhostSpawnRules(
            EntityType<GhostEntity> type,
            ServerLevelAccessor level,
            MobSpawnType reason,
            BlockPos pos,
            RandomSource random) {

        // 明るさ0かつY座標が50以下（地下）でのみスポーン
        return pos.getY() < 50
                && level.getRawBrightness(pos, 0) == 0
                && checkMobSpawnRules(type, level, reason, pos, random);
    }

    // ──────────────────────────────────────────
    // GhostFloatGoal：ターゲットの頭上あたりをふわふわ移動
    // ──────────────────────────────────────────
    static class GhostFloatGoal extends Goal {

        private final GhostEntity ghost;
        private int timer = 0;
        private double floatPhase = 0;
        private double wanderX = 0;
        private double wanderY = 0;
        private double wanderZ = 0;

        GhostFloatGoal(GhostEntity ghost) {
            this.ghost = ghost;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return ghost.getTarget() == null && ghost.getAttackState() == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return ghost.getTarget() == null && ghost.getAttackState() == 0;
        }

        @Override
        public void start() {
            timer = 0;
            pickNewWanderTarget();
        }

        @Override
        public void tick() {
            timer++;
            floatPhase += 0.05;

            // 60tickごとに新しい目標位置を選ぶ
            if (timer % 60 == 0) {
                pickNewWanderTarget();
            }

            // Y方向にサイン波でゆらゆら
            double yTarget = wanderY + Math.sin(floatPhase) * 0.5;

            // GhostMoveControlに目標位置を渡して滑らかに移動
            ghost.getMoveControl().setWantedPosition(
                    wanderX, yTarget, wanderZ, 0.5D);

            // 地面に近づきすぎたら浮き上がる
            BlockPos below = ghost.blockPosition().below();
            if (ghost.level().getBlockState(below).isSolid()
                    && ghost.getDeltaMovement().y < 0) {
                ghost.setDeltaMovement(
                        ghost.getDeltaMovement().x,
                        0.05,
                        ghost.getDeltaMovement().z
                );
            }
        }

        private void pickNewWanderTarget() {
            // 現在地から±8ブロック以内のランダムな位置を目標にする
            wanderX = ghost.getX() + (ghost.random.nextDouble() - 0.5) * 16;
            wanderY = ghost.getY() + (ghost.random.nextDouble() - 0.5) * 4;
            wanderZ = ghost.getZ() + (ghost.random.nextDouble() - 0.5) * 16;
        }
    }

    // ──────────────────────────────────────────
    // GhostAttackGoal
    // ──────────────────────────────────────────
    static class GhostAttackGoal extends Goal {

        private final GhostEntity mob;
        private LivingEntity target;
        private int attackTimer = 0;
        private int cooldown    = 0;

        private static final double MELEE_RANGE_SQ  = 9.0D;  // 3ブロック以内で近接
        private static final double RANGED_RANGE_SQ = 100.0D; // 10ブロック以内で遠距離

        GhostAttackGoal(GhostEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = mob.getTarget();
            return t != null && t.isAlive();
        }

        @Override
        public void start() {
            this.target   = mob.getTarget();
            this.attackTimer = 0;
            this.cooldown    = 0;
        }

        @Override
        public void stop() {
            mob.setAttackState(0);
            this.attackTimer = 0;
            this.cooldown = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void tick() {
            LivingEntity t = mob.getTarget();
            if (t == null || !t.isAlive()) {
                mob.setAttackState(0);
                return;
            }
            this.target = t;
            mob.getLookControl().setLookAt(t, 30F, 30F);

            // 攻撃中は移動しない
            if (mob.getAttackState() > 0) {
                attackTimer++;
                mob.setDeltaMovement(mob.getDeltaMovement().scale(0.8));
                executeAttack(t);
                return;
            }

            if (cooldown > 0) {
                cooldown--;
                approachTarget(t);
                return;
            }

            double distSq = mob.distanceToSqr(t);

            if (distSq <= MELEE_RANGE_SQ) {
                // 近接射程内：近接攻撃
                mob.setAttackState(1);
                attackTimer = 0;
            } else if (distSq <= RANGED_RANGE_SQ) {
                // 遠距離射程内：確率で遠距離攻撃、それ以外は接近
                if (mob.random.nextInt(3) == 0) {
                    mob.setAttackState(2);
                    attackTimer = 0;
                } else {
                    approachTarget(t); // ← 接近しながら機会を伺う
                }
            } else {
                approachTarget(t);
            }
        }
        @Override
        public boolean canContinueToUse() {
            LivingEntity t = mob.getTarget();
            return t != null && t.isAlive();
        }

        private void approachTarget(LivingEntity t) {
            double dist = mob.distanceTo(t);
            // 遠いときは相手の少し上、近いときは同じ高さ
            double targetY = dist > 8
                    ? t.getY() + 2.0
                    : t.getY() + 1.0;

            mob.getMoveControl().setWantedPosition(
                    t.getX(),
                    targetY,
                    t.getZ(),
                    1.2D
            );
        }

        private void executeAttack(LivingEntity t) {
            switch (mob.getAttackState()) {

                // ── attack1：近接（15tick、10tickで判定）──
                case 1 -> {
                    if (attackTimer == ATK1_HIT) {
                        doMeleeAttack(t);
                    }
                    if (attackTimer >= ATK1_END) {
                        finishAttack(20);
                    }
                }

                // ── attack2：遠距離（40tick、20tickで判定）──
                case 2 -> {
                    if (attackTimer == ATK2_HIT) {
                        doRangedAttack(t);
                    }
                    if (attackTimer >= ATK2_END) {
                        finishAttack(30);
                    }
                }
            }
        }

        // 近接：両腕で引っ掻く
        private void doMeleeAttack(LivingEntity t) {
            if (!t.isAlive()) return;

            AABB box = mob.getBoundingBox().inflate(2.5, 1.0, 2.5);
            // ★ ターゲットが範囲内にいるかだけ確認
            if (!box.intersects(t.getBoundingBox())) return;

            t.invulnerableTime = 0;
            t.hurt(mob.damageSources().mobAttack(mob),
                    (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE));
            Vec3 kb = t.position().subtract(mob.position()).normalize().scale(1.5);
            t.setDeltaMovement(kb.x, 0.4, kb.z);
            t.hurtMarked = true;

            mob.playSound(SoundEvents.PHANTOM_SWOOP, 1.0F, 0.8F);
        }

        // 遠距離：呪いの弾（ゴーストがターゲットへ突進してすり抜け）
        private void doRangedAttack(LivingEntity t) {
            if (mob.level().isClientSide()) return;

            Vec3 dir = t.position()
                    .add(0, t.getBbHeight() * 0.5, 0)
                    .subtract(mob.position())
                    .normalize();

            mob.setDeltaMovement(dir.scale(1.2));

            if (mob.level() instanceof ServerLevel sl) {
                sl.getServer().tell(new net.minecraft.server.TickTask(
                        sl.getServer().getTickCount() + 5,
                        () -> {
                            if (!mob.isAlive() || !t.isAlive()) return;
                            AABB hitBox = mob.getBoundingBox().inflate(1.5);
                            // ★ ターゲットだけにダメージを与える
                            if (hitBox.intersects(t.getBoundingBox())) {
                                t.invulnerableTime = 0;
                                t.hurt(mob.damageSources().mobAttack(mob),
                                        (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.1F);
                                t.hurtMarked = true;
                            }
                            // パーティクルはそのまま
                            sl.sendParticles(
                                    net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                                    mob.getX(), mob.getY() + 1.0, mob.getZ(),
                                    20, 0.5, 0.5, 0.5, 0.05
                            );
                        }
                ));
            }
            mob.playSound(SoundEvents.AMBIENT_CAVE.get(), 1.5F, 1.2F);
        }

        private void finishAttack(int cd) {
            attackTimer = 0;
            cooldown    = cd;
            mob.setAttackState(0);
            mob.setDeltaMovement(Vec3.ZERO);
        }
    }

    // ──────────────────────────────────────────
    // GeckoLib アニメーション
    // ──────────────────────────────────────────
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 3, state -> {

            int atk = this.getAttackState();

            // 前フレームと状態が変わったときだけリセット
            if (atk == 1) {
                return state.setAndContinue(
                        RawAnimation.begin().thenPlay("attack1"));
            }
            if (atk == 2) {
                return state.setAndContinue(
                        RawAnimation.begin().thenPlay("attack2"));
            }

            return state.setAndContinue(
                    RawAnimation.begin().thenLoop("idle"));
        }));
    }
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }
}