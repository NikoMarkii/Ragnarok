package com.niko.ragnarok.entity.costom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class Groot extends Monster {
    private int attackTick = 0;
    private int attackType = 0;
    private boolean isAttacking = false;
    private int deathTime = 0;
    private boolean isDying = false;

    public boolean isAttackingExternal() {
        return this.isAttacking;
    }

    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState attack1AnimationState = new AnimationState();
    public final AnimationState walkAnimationState = new AnimationState();
    public final AnimationState deathAnimationState = new AnimationState();
    public final AnimationState attack2AnimationState = new AnimationState();
    private static final byte START_ATTACK_2_EVENT = 6; // 新しいイベントID
    private static final byte START_ATTACK_1_EVENT = 4;
    private static final byte START_DEATH_EVENT = 5;

    private static final EntityDataAccessor<Boolean> IS_ANGRY =
            SynchedEntityData.defineId(Groot.class, EntityDataSerializers.BOOLEAN);

    public Groot(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IS_ANGRY, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new GrootMeleeAttackGoal(this, 0.15D, false));
        this.goalSelector.addGoal(2, new GrootChargeGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                (entity) -> this.isAngry()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 2.0D);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == START_ATTACK_1_EVENT) {
            this.idleAnimationState.stop();
            this.attack1AnimationState.start(this.tickCount);
        } else if (id == START_ATTACK_2_EVENT) { // else if にする
            this.idleAnimationState.stop();
            this.attack2AnimationState.start(this.tickCount);
        } else if (id == START_DEATH_EVENT) {
            this.deathAnimationState.start(this.tickCount);
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();

        // 死亡アニメーション処理
        if (isDying) {
            deathTime++;
            // 2.3秒 (46チック) でアニメーション終了
            if (deathTime >= 46) {
                this.remove(RemovalReason.KILLED);
            }
            return;
        }

        if (isAttacking) {
            attackTick++;

            // 攻撃タイプ別の処理
            switch (attackType) {
                // Groot.java の aiStep内
                case 0: // 通常殴り
                    // 1.25秒 = 25チックの瞬間に攻撃判定を発生させる
                    if (attackTick == 25) {
                        performNormalAttack();
                    }
                    // アニメーションの長さ 2.0秒 = 40チックで終了
                    if (attackTick >= 40) {
                        endAttack();
                    }
                    break;

                case 1: // 叩き付け (attack2)
                    // 1.74秒 = 約35チックの瞬間に衝撃波を発生させる
                    if (attackTick == 35) {
                        performSlamAttack();
                        spawnWaveBlocks(); // 波のようなエフェクト（後述）
                    }
                    // 2.38秒 = 約48チックで終了
                    if (attackTick >= 48) {
                        endAttack();
                    }
                    break;

                case 2: // 突進 (10ダメージ)
                    if (attackTick >= 5 && attackTick <= 25) {
                        performChargeAttack();
                    }
                    if (attackTick >= 35) {
                        endAttack();
                    }
                    break;
            }
        }

        // 周囲の動物が殺されるのを監視
        if (!this.level().isClientSide && !isAngry() && this.tickCount % 20 == 0) {
            checkNearbyAnimalDeaths();
        }
    }

    private void performNormalAttack() {
        // 自身の向きに合わせて前方に力を加える
        double d0 = Math.sin(this.getYRot() * (Math.PI / 180.0));
        double d1 = -Math.cos(this.getYRot() * (Math.PI / 180.0));
        // 0.5Dをもう少し強めてもいいかもしれないね
        this.setDeltaMovement(this.getDeltaMovement().add(d0 * 0.8D, 0.0D, d1 * 0.8D));

        LivingEntity target = this.getTarget();
        // 踏み込む分、判定距離(3.5D)を少し広めに設定しておくと空振りが減るよ
        if (target != null && this.distanceTo(target) < 4.5D) {
            target.hurt(this.damageSources().mobAttack(this), 8.0F);
            this.playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 1.0F);
        }
    }

    // 叩き付け攻撃 (ブロック破壊 + 範囲ダメージ)
    public void performSlamAttack() {
        if (!this.level().isClientSide) {
            // ブロック破壊範囲 (3x3x3)
            BlockPos center = this.blockPosition().relative(this.getDirection());
            BlockPos.betweenClosedStream(center.offset(-1, 0, -1), center.offset(1, 2, 1)).forEach(pos -> {
                BlockState state = this.level().getBlockState(pos);
                if (state.getDestroySpeed(this.level(), pos) >= 0 && state.getDestroySpeed(this.level(), pos) <= 2.0F) {
                    this.level().destroyBlock(pos, true, this);
                }
            });

            // ダメージ範囲 (5x5x3) - ブロック破壊より広い
            AABB damageBox = new AABB(
                    this.getX() - 2.5D, this.getY(), this.getZ() - 2.5D,
                    this.getX() + 2.5D, this.getY() + 3.0D, this.getZ() + 2.5D
            );

            List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class, damageBox,
                    entity -> entity != this && entity.isAlive());

            for (LivingEntity entity : entities) {
                entity.hurt(this.damageSources().mobAttack(this), 14.0F);
                // ノックバック
                double dx = entity.getX() - this.getX();
                double dz = entity.getZ() - this.getZ();
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > 0) {
                    entity.setDeltaMovement(entity.getDeltaMovement().add(
                            dx / distance * 0.8D,
                            0.4D,
                            dz / distance * 0.8D
                    ));
                }
            }

            this.playSound(SoundEvents.GENERIC_EXPLODE, 1.5F, 0.8F);
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
            // クリエイティブモードとスペクテイターモードを除外する判定を追加
            if (!player.isCreative() && !player.isSpectator()) {
                // ここに「もし動物を殺していたら」のロジックを入れる
                // 以前話した通り、イベントハンドラー経由で onNearbyAnimalKilled を呼ぶのがスマートだよ
            }
        }
    }

    // 外部（Forgeのイベントハンドラーなど）から呼ばれるメソッドも修正
    public void onNearbyAnimalKilled(Player killer) {
        // 殺人者がプレイヤーであり、かつサバイバル/アドベンチャーモードの場合のみ敵対
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
        if (!this.level().isClientSide && !isDying) {
            isDying = true;
            deathTime = 0;
            this.level().broadcastEntityEvent(this, START_DEATH_EVENT);

            // ドロップアイテム
            dropCustomLoot();
        }
    }
    @Override
    public void travel(Vec3 pTravelVector) {
        if (this.isAttacking && this.attackType == 1) {
            // 叩きつけ中は移動ベクトルをゼロにする
            super.travel(Vec3.ZERO);
        } else {
            super.travel(pTravelVector);
        }
    }

    private void dropCustomLoot() {
        if (this.level() instanceof ServerLevel) {
            // 原木 10-20個
            int logCount = 10 + this.random.nextInt(11);
            for (int i = 0; i < logCount; i++) {
                this.spawnAtLocation(Items.OAK_LOG);
            }

            // ツタ 0-4個
            int vineCount = this.random.nextInt(5);
            for (int i = 0; i < vineCount; i++) {
                this.spawnAtLocation(Items.VINE);
            }

            // 「冷えた心の核」を確定ドロップ
            // TODO: ModItems.CHILLED_HEART_CORE に置き換える
            // 現在は仮でNether Starを使用
            ItemStack heart = new ItemStack(Items.NETHER_STAR);
            heart.setHoverName(net.minecraft.network.chat.Component.literal("冷えた心の核"));

            ItemEntity itemEntity = new ItemEntity(this.level(),
                    this.getX(), this.getY() + 1.0D, this.getZ(), heart);
            this.level().addFreshEntity(itemEntity);
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
                // 死亡アニメーション中はアイドルと歩きを停止
                this.idleAnimationState.stop();
                this.walkAnimationState.stop();
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
            for (int r = 1; r <= 3; r++) { // 半径を広げていく
                final int radius = r;
                // チックをずらして実行することで「波」を表現する
                // (簡易的な実装として、ここでは位置の計算のみ)
                for (double i = 0; i < Math.PI * 2; i += Math.PI / 4) {
                    double dx = Math.cos(i) * radius;
                    double dz = Math.sin(i) * radius;
                    BlockPos pos = center.offset((int)dx, -1, (int)dz);

                    // 実際にはここでブロックの破片パーティクルを出すのが軽量だよ
                    ((ServerLevel)this.level()).sendParticles(
                            new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, this.level().getBlockState(pos)),
                            pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                            5, 0.1, 0.5, 0.1, 0.15
                    );
                }
            }
        }
    }

    // 死亡アニメーション中は無敵
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isDying) {
            return false;
        }
        return super.hurt(source, amount);
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
            // super.tick() を呼ばないことで、バニラの自動攻撃を完全にスキップするよ

            LivingEntity target = this.groot.getTarget();
            if (target != null) {
                // ターゲットを注視させる
                this.groot.getLookControl().setLookAt(target, 30.0F, 30.0F);

                // ターゲットへの距離を計算
                double distanceSq = this.groot.distanceToSqr(target.getX(), target.getY(), target.getZ());

                // ターゲットへ移動する (groot経由で呼び出す)
                this.groot.getNavigation().moveTo(target, 1.0D);

                // 自前の攻撃ロジック
                // isAttacking() メソッド（後述）を介して状態をチェックするよ
                if (distanceSq < 12.25D && attackCooldown <= 0 && !this.groot.isAttackingExternal()) {
                    if (this.groot.getRandom().nextFloat() < 0.3F) {
                        this.groot.startSlamAttack();
                        attackCooldown = 100;
                    } else {
                        this.groot.startNormalAttack();
                        attackCooldown = 40;
                    }
                }
            }

            if (attackCooldown > 0) {
                attackCooldown--;
            }
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity pEnemy, double pDistToEnemySqr) {
            // ここを空のままにしておくことで、バニラの勝手なダメージ判定を阻止するよ
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