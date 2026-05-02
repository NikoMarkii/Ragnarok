package com.niko.ragnarok.entity.costom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.niko.ragnarok.item.Ragnarok_mainItems;
import net.minecraft.util.RandomSource;

import java.util.List;

public class Groot extends Animal {
    private int attackTick = 0;
    private int attackType = 0;
    private boolean isAttacking = false;
    private boolean isDying = false;
    private boolean animationStarted = false;

    private int customDeathTime = 0;

    public boolean isAttackingExternal() {
        return this.isAttacking;
    }

    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState attack1AnimationState = new AnimationState();
    public final AnimationState walkAnimationState = new AnimationState();
    public final AnimationState deathAnimationState = new AnimationState();
    public final AnimationState attack2AnimationState = new AnimationState();
    private static final byte START_ATTACK_2_EVENT = 6;
    private static final byte START_ATTACK_1_EVENT = 4;
    private static final byte START_DEATH_EVENT = 5;

    private static final EntityDataAccessor<Boolean> IS_ANGRY =
            SynchedEntityData.defineId(Groot.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> IS_DYING =
            SynchedEntityData.defineId(Groot.class, EntityDataSerializers.BOOLEAN);

    public Groot(EntityType<? extends Animal> type, Level level) {
        super(type, level);
        // 2ブロックの段差をパスとして認識させる
        this.getNavigation().setCanFloat(true);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IS_ANGRY, false);
        this.entityData.define(IS_DYING, false);
    }




    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new GrootMeleeAttackGoal(this, 0.9D, false));
        this.goalSelector.addGoal(2, new GrootChargeGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                (entity) -> this.isAngry()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.ATTACK_DAMAGE, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 2.0D)
                .add(Attributes.JUMP_STRENGTH, 1.0D);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == START_ATTACK_1_EVENT) {
            this.idleAnimationState.stop();
            this.attack1AnimationState.start(this.tickCount);
        } else if (id == START_ATTACK_2_EVENT) {
            this.idleAnimationState.stop();
            this.attack2AnimationState.start(this.tickCount);
        } else if (id == START_DEATH_EVENT) {
            // 死亡アニメーション開始（一度だけ）
            if (!animationStarted) {
                this.deathAnimationState.start(this.tickCount);
                this.animationStarted = true;
            }
        } else {
            super.handleEntityEvent(id);
        }
    }
    @Override
    protected float getJumpPower() {
        // 通常は 0.42F 程度。これを 0.8F 前後にすると 2.2ブロックほど跳べるようになるよ
        return 0.8F * this.getBlockJumpFactor();
    }


    @Override
    public void aiStep() {
        if (this.isActuallyDying()) {
            this.customDeathTime++;
            this.setDeltaMovement(Vec3.ZERO);

            if (!this.level().isClientSide && this.customDeathTime >= 46) {
                this.remove(RemovalReason.KILLED);
            }
            return;
        }

        super.aiStep();

        if (isAttacking) {
            attackTick++;
            switch (attackType) {
                case 0: // 通常殴り
                    if (attackTick == 10) performNormalAttack();
                    if (attackTick >= 20) endAttack();
                    break;
                case 1: // 叩き付け
                    if (attackTick == 30) {
                        performSlamAttack();
                        spawnWaveBlocks();
                    }
                    if (attackTick >= 48) endAttack();
                    break;
                case 2: // 突進
                    if (attackTick >= 5 && attackTick <= 25) performChargeAttack();
                    if (attackTick >= 35) endAttack();
                    break;
            }
        }

        if (!this.level().isClientSide && !isAngry() && this.tickCount % 20 == 0) {
            checkNearbyAnimalDeaths();
        }
    }

    private void performNormalAttack() {
        LivingEntity target = this.getTarget();
        // 判定距離は 4.0D 程度で維持
        if (target != null && this.distanceTo(target) < 5.0D) {
            // 1. ダメージを与える
            target.hurt(this.damageSources().mobAttack(this), 8.0F);

            // 2. 強力なノックバックを付与
            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            double distance = Math.max(0.1D, Math.sqrt(dx * dx + dz * dz));

            // 横方向に強く (1.2D)、少し上方向へ (0.3D) 飛ばす
            target.setDeltaMovement(target.getDeltaMovement().add(
                    dx / distance * 1.2D,
                    0.3D,
                    dz / distance * 1.2D
            ));

            this.playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 1.0F);
        }
    }

    // 叩き付け攻撃 (ブロック破壊 + 範囲ダメージ)
    public void performSlamAttack() {
        if (!this.level().isClientSide) {
            BlockPos center = this.blockPosition();

            // --- 1. ブロック破壊ロジック (13x13) ---
            // 視覚的な「爆心地」の破壊範囲
            int breakRadius = 6; // 中心から6ブロック（計13x13）
            BlockPos.betweenClosedStream(
                    center.offset(-breakRadius, 0, -breakRadius),
                    center.offset(breakRadius, 2, breakRadius)
            ).forEach(pos -> {
                BlockState state = this.level().getBlockState(pos);
                if (state.getDestroySpeed(this.level(), pos) >= 0 && state.getDestroySpeed(this.level(), pos) <= 2.0F) {
                    this.level().destroyBlock(pos, true, this);
                }
            });

            // --- 2. ダメージ判定範囲 (17x17) ---
            // 破壊範囲よりも広く設定 (半径8.5D = 約17x17)
            double damageRadius = 8.5D;
            AABB damageBox = new AABB(
                    this.getX() - damageRadius, this.getY(), this.getZ() - damageRadius,
                    this.getX() + damageRadius, this.getY() + 3.0D, this.getZ() + damageRadius
            );

            List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class, damageBox,
                    entity -> entity != this && entity.isAlive());

            for (LivingEntity entity : entities) {
                // 中心（グルート）に近いほど大ダメージにする工夫もできるけれど、
                // ひとまずは一律でウォーデン級の重い一撃を。
                entity.hurt(this.damageSources().mobAttack(this), 14.0F);

                // 吹き飛ばしベクトル
                double dx = entity.getX() - this.getX();
                double dz = entity.getZ() - this.getZ();
                double distance = Math.max(0.1D, Math.sqrt(dx * dx + dz * dz));

                // 遠くにいる敵ほど、衝撃波で外側へ強く弾き飛ばそう
                entity.setDeltaMovement(entity.getDeltaMovement().add(
                        dx / distance * 1.8D,
                        0.6D,
                        dz / distance * 1.8D
                ));
            }

            // --- 3. 演出の調整 ---
            this.playSound(SoundEvents.GENERIC_EXPLODE, 2.0F, 0.5F);

            if (this.level() instanceof ServerLevel serverLevel) {
                // 爆発エフェクトは破壊範囲に合わせつつ、
                // 広いダメージ範囲には土煙（クラウドパーティクル）などを散らすと、「見えない衝撃波」が伝わった感じが出るよ
                serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(), this.getZ(), 5, 3.0D, 0.5D, 3.0D, 0.1D);
                serverLevel.sendParticles(ParticleTypes.POOF, this.getX(), this.getY(), this.getZ(), 40, 7.0D, 0.2D, 7.0D, 0.05D);
            }
        }
    }

    // 突進攻撃
    private void performChargeAttack() {
        LivingEntity target = this.getTarget();
        if (target != null) {
            // ターゲットに向かって突進
            Vec3 direction = target.position().subtract(this.position()).normalize();
            this.setDeltaMovement(direction.x * 0.6D, this.getDeltaMovement().y, direction.z * 0.6D);

            // 接触したらダメージ
            if (this.distanceTo(target) < 2.0D) {
                target.hurt(this.damageSources().mobAttack(this), 10.0F);
                endAttack();
            }
        }
    }

    // 攻撃開始 (通常殴り)
    public void startNormalAttack() {
        startAttack(0);
    }

    // 攻撃開始 (叩き付け)
    public void startSlamAttack() {
        startAttack(1);
    }

    // 攻撃開始 (突進)
    public void startChargeAttack() {
        startAttack(2);
    }

    private void startAttack(int type) {
        if (!isAttacking && !isDying) {
            isAttacking = true;
            attackType = type;
            attackTick = 0;

            // 攻撃タイプに応じたイベントを送信
            byte eventId;
            if (type == 1) {
                eventId = START_ATTACK_2_EVENT; // 叩きつけ
            } else if (type == 2) {
                eventId = START_ATTACK_1_EVENT; // 突進用（必要なら専用IDを）
            } else {
                eventId = START_ATTACK_1_EVENT; // 通常
            }

            this.level().broadcastEntityEvent(this, eventId);
        }
    }

    private void endAttack() {
        isAttacking = false;
        attackTick = 0;
        attackType = 0;
    }

    // 周囲の動物が殺されるのを監視
    private void checkNearbyAnimalDeaths() {
        AABB searchBox = new AABB(
                this.getX() - 16.0D, this.getY() - 8.0D, this.getZ() - 16.0D,
                this.getX() + 16.0D, this.getY() + 8.0D, this.getZ() + 16.0D
        );

        List<Player> players = this.level().getEntitiesOfClass(Player.class, searchBox);
        for (Player player : players) {
            if (!player.isCreative() && !player.isSpectator()) {
            }
        }
    }

    public void onNearbyAnimalKilled(Player killer) {
        if (!isAngry() && killer != null && !killer.isCreative() && !killer.isSpectator()) {
            setAngry(true);
            this.setTarget(killer);
            this.playSound(SoundEvents.RAVAGER_ROAR, 2.0F, 0.8F);
        }
    }

    public boolean isAngry() {
        return this.entityData.get(IS_ANGRY);
    }

    public void setAngry(boolean angry) {
        this.entityData.set(IS_ANGRY, angry);
    }

    @Override
    public void die(DamageSource damageSource) {
        if (!this.level().isClientSide) {
            this.entityData.set(IS_DYING, true); // ここで同期
            this.level().broadcastEntityEvent(this, START_DEATH_EVENT);
            dropCustomLoot();
        }
    }
    @Override
    public void travel(Vec3 pTravelVector) {
        if (this.isAttacking && this.attackType == 1) {
            super.travel(Vec3.ZERO);
        } else {
            super.travel(pTravelVector);
        }
    }

    private void dropCustomLoot() {
        if (this.level() instanceof ServerLevel) {
            int logCount = 10 + this.random.nextInt(11);
            for (int i = 0; i < logCount; i++) {
                this.spawnAtLocation(Items.OAK_LOG);
            }
            int vineCount = this.random.nextInt(5);
            for (int i = 0; i < vineCount; i++) {
                this.spawnAtLocation(Items.VINE);
            }
            this.spawnAtLocation(Ragnarok_mainItems.GROOT_HARHT.get());
        }
    }

    @Override
    public float getStepHeight() {
        return 2.0F;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            if (isDying) {
                // 死亡中は他のアニメーションを停止
                this.idleAnimationState.stop();
                this.walkAnimationState.stop();
                this.attack1AnimationState.stop();
                this.attack2AnimationState.stop();

                // 死亡アニメーションを開始（クライアント側で確実に実行）
                this.deathAnimationState.startIfStopped(this.tickCount);
            } else {
                if (this.getDeltaMovement().horizontalDistanceSqr() > 0.0001D) {
                    this.walkAnimationState.startIfStopped(this.tickCount);
                } else {
                    this.walkAnimationState.stop();
                }
                this.idleAnimationState.startIfStopped(this.tickCount);
            }
        }
    }
    private void spawnWaveBlocks() {
        if (!this.level().isClientSide) {
            BlockPos center = this.blockPosition().relative(this.getDirection());
            for (int r = 1; r <= 3; r++) {
                final int radius = r;

                for (double i = 0; i < Math.PI * 2; i += Math.PI / 4) {
                    double dx = Math.cos(i) * radius;
                    double dz = Math.sin(i) * radius;
                    BlockPos pos = center.offset((int)dx, -1, (int)dz);

                    ((ServerLevel)this.level()).sendParticles(
                            new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, this.level().getBlockState(pos)),
                            pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                            5, 0.1, 0.5, 0.1, 0.15
                    );
                }
            }
        }
    }
    public static boolean checkGrootSpawnRules(EntityType<Groot> pGroot, LevelAccessor pLevel, MobSpawnType pSpawnType, BlockPos pPos, RandomSource pRandom) {
        // 明るさ制限なし - 昼夜問わずスポーン可能
        return pLevel.getBlockState(pPos.below()).is(BlockTags.DIRT);
    }
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isDying) {
            return false;
        }
        return super.hurt(source, amount);
    }
    public boolean isActuallyDying() {
        return this.entityData.get(IS_DYING);
    }

    // バニラの死亡時の倒れ込みを無効化
    @Override
    public boolean shouldDropExperience() {
        return !this.isBaby();
    }

    @Override
    public boolean isDeadOrDying() {
        return this.isDying || super.isDeadOrDying();
    }

    @Override
    protected void tickDeath() {
    }

    // カスタム死亡タイマー用のゲッター（オーバーライドではない）
    public int getCustomDeathTime() {
        return this.customDeathTime;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource pDamageSource) {
        // ダメージを受けた時の音をアイアンゴーレムに設定
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        // 死亡時の音もアイアンゴーレムに合わせると統一感が出るよ
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pPos, BlockState pState) {
        this.playSound(SoundEvents.IRON_GOLEM_STEP, 1.0F, 1.0F);
    }

    // Animalクラスの必須メソッド(繁殖しないのでnullを返す)
    @Override
    public Animal getBreedOffspring(net.minecraft.server.level.ServerLevel pLevel, net.minecraft.world.entity.AgeableMob pOtherParent) {
        return null;
    }
}

class GrootMeleeAttackGoal extends MeleeAttackGoal {
    private final Groot groot;
    private int attackCooldown = 0;

    public GrootMeleeAttackGoal(Groot groot, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        super(groot, speedModifier, followingTargetEvenIfNotSeen);
        this.groot = groot;
    }
    @Override
    public void tick() {
        LivingEntity target = this.groot.getTarget();
        if (target == null) return;

        // 常にターゲットの方を向く
        this.groot.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // 攻撃中でも、移動（ナビゲーション）を止めない！
        // 1.0D（等倍）で常にターゲットを追いかけ続ける
        this.groot.getNavigation().moveTo(target, 1.0D);

        double distanceSq = this.groot.distanceToSqr(target.getX(), target.getY(), target.getZ());

        // クールダウン中でなければ攻撃を開始
        if (distanceSq < 12.25D && attackCooldown <= 0 && !this.groot.isAttackingExternal()) {
            if (this.groot.getRandom().nextFloat() < 0.3F) {
                this.groot.startSlamAttack();
                attackCooldown = 30; // 1.5秒に短縮
            } else {
                this.groot.startNormalAttack();
                attackCooldown = 10;
            }
        }

        if (attackCooldown > 0) {
            attackCooldown--;
        }
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity pEnemy, double pDistToEnemySqr) {
    }
}
// カスタム突進ゴール
class GrootChargeGoal extends Goal {
    private final Groot groot;
    private int chargeCooldown = 0;

    public GrootChargeGoal(Groot groot) {
        this.groot = groot;
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.groot.getTarget();
        return target != null &&
                this.groot.distanceTo(target) > 8.0D &&
                this.groot.distanceTo(target) < 20.0D &&
                chargeCooldown <= 0 &&
                this.groot.isAngry();
    }

    @Override
    public void start() {
        this.groot.startChargeAttack();
        chargeCooldown = 200; // 10秒クールダウン
    }

    @Override
    public void tick() {
        if (chargeCooldown > 0) {
            chargeCooldown--;
        }
    }
}