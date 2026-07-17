package com.niko.ragnarok.entity.costom;

import com.niko.ragnarok.Ragnarok;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;

public class Magic_Golem extends Raider {

    // アニメーション状態
    public AnimationState idleAnimationState = new AnimationState();
    public AnimationState walkAnimationState = new AnimationState();
    public AnimationState attack1AnimationState = new AnimationState();
    public AnimationState attack2AnimationState = new AnimationState();
    public AnimationState summonAnimationState = new AnimationState();

    // データシンク
    private static final EntityDataAccessor<Boolean> IS_SUMMONING =
            SynchedEntityData.defineId(Magic_Golem.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ATTACK_TYPE =
            SynchedEntityData.defineId(Magic_Golem.class, EntityDataSerializers.INT);

    // 攻撃関連
    private static final byte START_NORMAL_ATTACK_EVENT = 4;   // 通常攻撃
    private static final byte START_SWING_ATTACK_EVENT = 5;    // 腕ぶん回し
    private static final byte START_SUMMON_ATTACK_EVENT = 6;   // 召喚攻撃
    private static final byte START_CHARGE_ATTACK_EVENT = 7;   // 突進攻撃

    private static final byte STOP_ALL_ATTACKS_EVENT = 8;

    private int attackCooldown = 0;
    private int attackTicks = 0;
    private int attackType = 0; // 0=なし, 1=通常, 2=腕ぶん回し, 3=召喚, 4=突進
    private boolean isAttacking = false;
    private int summonCooldown = 0;
    private int chargeTicks = 0;
    private Vec3 chargeDirection = Vec3.ZERO;

    public Magic_Golem(EntityType<? extends Raider> type, Level level) {
        super(type, level);
        this.xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 150.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.ARMOR, 15.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IS_SUMMONING, false);
        this.entityData.define(ATTACK_TYPE, 0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MagicGolemMeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(3, new MagicGolemMoveToRaidGoal<>(this));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Villager.class, true));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }
    @Override
    public void applyRaidBuffs(int wave, boolean unused) {
    }



    public boolean isSummoning() {
        return this.entityData.get(IS_SUMMONING);
    }

    public void setSummoning(boolean summoning) {
        this.entityData.set(IS_SUMMONING, summoning);
    }

    public int getAttackType() {
        return this.entityData.get(ATTACK_TYPE);
    }

    public void setAttackType(int type) {
        this.entityData.set(ATTACK_TYPE, type);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        // 攻撃処理
        if (attackCooldown > 0) {
            attackCooldown--;
        }

        if (summonCooldown > 0) {
            summonCooldown--;
        }

        if (isAttacking) {
            attackTicks++;

            // 攻撃タイプ別処理
            switch (attackType) {
                case 1: // 通常攻撃 (23チック = 1.15秒)
                    if (attackTicks == 14) { // 0.7秒地点でダメージ
                        performNormalAttack();
                    }
                    if (attackTicks >= 23) {
                        endAttack();
                    }
                    break;

                case 2: // 腕ぶん回し (26チック = 1.3秒)
                    if (attackTicks == 14) { // 0.7秒地点でダメージ
                        performSwingAttack();
                    }
                    if (attackTicks >= 26) {
                        endAttack();
                    }
                    break;

                case 3: // 召喚攻撃 (ファング) (26チック = 1.3秒)
                    if (attackTicks == 10) { // 0.5秒地点でファング召喚
                        performSummonAttack();
                    }
                    if (attackTicks >= 26) {
                        endAttack();
                    }
                    break;

                case 4: // 突進 (60チック = 3秒)
                    if (attackTicks <= 40) {
                        performCharge();
                    }
                    if (attackTicks >= 60) {
                        endAttack();
                    }
                    break;
            }
        }

        // パーティクル効果
        if (this.level().isClientSide && this.random.nextFloat() < 0.1F) {
            this.level().addParticle(ParticleTypes.ENCHANT,
                    this.getX() + (this.random.nextDouble() - 0.5D) * this.getBbWidth(),
                    this.getY() + this.random.nextDouble() * this.getBbHeight(),
                    this.getZ() + (this.random.nextDouble() - 0.5D) * this.getBbWidth(),
                    0.0D, 0.05D, 0.0D);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            if (this.getDeltaMovement().horizontalDistanceSqr() > 0.0001D) {
                this.walkAnimationState.startIfStopped(this.tickCount);
                this.idleAnimationState.stop();
            } else {
                this.walkAnimationState.stop();
                this.idleAnimationState.startIfStopped(this.tickCount);
            }
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == START_NORMAL_ATTACK_EVENT) {
            this.attack1AnimationState.start(this.tickCount);
            this.idleAnimationState.stop();
        } else if (id == START_SWING_ATTACK_EVENT) {
            this.attack2AnimationState.start(this.tickCount);
            this.idleAnimationState.stop();
        } else if (id == START_SUMMON_ATTACK_EVENT) {
            this.summonAnimationState.start(this.tickCount);
            this.idleAnimationState.stop();
        } else if (id == STOP_ALL_ATTACKS_EVENT) {
            // 全ての攻撃アニメーションを停止させる
            this.attack1AnimationState.stop();
            this.attack2AnimationState.stop();
            this.summonAnimationState.stop();
        } else {
            super.handleEntityEvent(id);
        }
    }

    // 通常攻撃を開始
    public void startNormalAttack() {
        this.isAttacking = true;
        this.attackType = 1;
        this.attackTicks = 0;
        this.setAttackType(1);
        this.level().broadcastEntityEvent(this, START_NORMAL_ATTACK_EVENT);
        this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 1.2F);
    }

    // 腕ぶん回し攻撃を開始
    public void startSwingAttack() {
        this.isAttacking = true;
        this.attackType = 2;
        this.attackTicks = 0;
        this.setAttackType(2);
        this.level().broadcastEntityEvent(this, START_SWING_ATTACK_EVENT);
        this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 0.8F);
    }

    // 召喚攻撃を開始
    public void startSummonAttack() {
        this.isAttacking = true;
        this.attackType = 3;
        this.attackTicks = 0;
        this.setAttackType(3);
        this.level().broadcastEntityEvent(this, START_SUMMON_ATTACK_EVENT);
        this.playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 1.0F, 1.0F);
    }

    // 突進攻撃を開始
    public void startChargeAttack() {
        this.isAttacking = true;
        this.attackType = 4;
        this.attackTicks = 0;
        this.chargeTicks = 0;
        this.setAttackType(4);

        LivingEntity target = this.getTarget();
        if (target != null) {
            Vec3 direction = target.position().subtract(this.position()).normalize();
            this.chargeDirection = new Vec3(direction.x, 0, direction.z);
        }
        this.level().broadcastEntityEvent(this, START_CHARGE_ATTACK_EVENT);
        this.playSound(SoundEvents.RAVAGER_ROAR, 1.5F, 1.0F);
    }

    // 通常攻撃の実行
    private void performNormalAttack() {
        AABB attackBox = this.getBoundingBox().inflate(2.0D, 1.0D, 2.0D);
        this.level().getEntitiesOfClass(LivingEntity.class, attackBox).forEach(entity -> {
            if (entity != this && entity.isAlive()) {
                entity.hurt(this.damageSources().mobAttack(this), 12.0F);
                // 軽めのノックバック
                double dx = entity.getX() - this.getX();
                double dz = entity.getZ() - this.getZ();
                entity.knockback(0.8D, -dx, -dz);
            }
        });
        this.playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 1.0F);
    }

    // 腕ぶん回し攻撃の実行
    private void performSwingAttack() {
        AABB attackBox = this.getBoundingBox().inflate(2.5D, 1.0D, 2.5D);
        this.level().getEntitiesOfClass(LivingEntity.class, attackBox).forEach(entity -> {
            if (entity != this && entity.isAlive()) {
                entity.hurt(this.damageSources().mobAttack(this), 15.0F);
                // ノックバック
                double dx = entity.getX() - this.getX();
                double dz = entity.getZ() - this.getZ();
                entity.knockback(1.5D, -dx, -dz);
            }
        });
        this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 0.8F);
    }

    private void performSummonAttack() {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            LivingEntity target = this.getTarget();
            if (target != null) {
                // 自分からターゲットへのベクトルを計算
                double dx = target.getX() - this.getX();
                double dz = target.getZ() - this.getZ();
                double distance = Math.sqrt(dx * dx + dz * dz); // ターゲットまでの距離

                // 調整項目：ファングを置く間隔（例：1.5ブロックごと）
                double stepDistance = 1.5D;

                // 進行方向の単位ベクトル
                double nx = dx / distance;
                double nz = dz / distance;

                // ターゲットの少し先（またはターゲットまで）に届くようにループ
                for (int i = 0; i < 12; i++) { // 12個のファングを並べる
                    // 現在の地点 ＝ 自分の位置 ＋ (方向 × (間隔 × i))
                    double x = this.getX() + nx * (stepDistance * i);
                    double z = this.getZ() + nz * (stepDistance * i);

                    // EvokerFangsを召喚
                    net.minecraft.world.entity.projectile.EvokerFangs fangs =
                            new net.minecraft.world.entity.projectile.EvokerFangs(
                                    EntityType.EVOKER_FANGS, this.level());
                    fangs.setOwner(this);
                    fangs.moveTo(x, this.getY(), z, 0.0F, 0.0F);
                    this.level().addFreshEntity(fangs);
                }
                this.playSound(SoundEvents.EVOKER_CAST_SPELL, 1.0F, 1.0F);
            }
        }
    }

    // 突進攻撃の実行
    private void performCharge() {
        chargeTicks++;

        // 最初の20チックは加速
        if (chargeTicks <= 20) {
            double speed = 0.05D * chargeTicks;
            this.setDeltaMovement(chargeDirection.scale(speed));
        } else {
            this.setDeltaMovement(chargeDirection.scale(1.0D));
        }

        // 当たり判定
        AABB chargeBox = this.getBoundingBox().inflate(0.5D);
        this.level().getEntitiesOfClass(LivingEntity.class, chargeBox).forEach(entity -> {
            if (entity != this && entity.isAlive()) {
                entity.hurt(this.damageSources().mobAttack(this), 10.0F);
                entity.knockback(2.0D, chargeDirection.x, chargeDirection.z);
            }
        });

        // パーティクル
        if (this.level().isClientSide) {
            for (int i = 0; i < 3; i++) {
                this.level().addParticle(ParticleTypes.CLOUD,
                        this.getX() + (this.random.nextDouble() - 0.5D),
                        this.getY() + 0.5D,
                        this.getZ() + (this.random.nextDouble() - 0.5D),
                        0.0D, 0.0D, 0.0D);
            }
        }
    }

    private void endAttack() {
        this.isAttacking = false;
        this.attackType = 0;
        this.attackTicks = 0;
        this.setAttackType(0);
        this.attackCooldown = 40;
        this.setDeltaMovement(Vec3.ZERO);
        this.chargeDirection = Vec3.ZERO;

        if (!this.level().isClientSide) {
            this.level().broadcastEntityEvent(this, STOP_ALL_ATTACKS_EVENT);
        }
    }

    public boolean isAttackingExternal() {
        return this.isAttacking;
    }

    @Override
    public ResourceLocation getDefaultLootTable() {
        return ResourceLocation.fromNamespaceAndPath(
                Ragnarok.MOD_ID, "entities/magic_golem");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        this.playSound(SoundEvents.IRON_GOLEM_STEP, 1.0F, 1.0F);
    }
    @Override
    public SoundEvent getCelebrateSound() {
        // 勝利した時の鳴き声
        return SoundEvents.RAVAGER_ROAR;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("AttackCooldown", this.attackCooldown);
        tag.putInt("SummonCooldown", this.summonCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.attackCooldown = tag.getInt("AttackCooldown");
        this.summonCooldown = tag.getInt("SummonCooldown");
    }

    @Override
    public float getStepHeight() {
        return 1.0F;
    }

    // スポーン条件
    public static boolean checkSpawnRules(EntityType<Magic_Golem> entityType, LevelAccessor level,
                                          MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(BlockTags.DIRT) ||
                level.getBlockState(pos.below()).is(BlockTags.STONE_ORE_REPLACEABLES);
    }

    static class MagicGolemMeleeAttackGoal extends Goal {
        private final Magic_Golem golem;
        private final double speedModifier;
        private int attackCooldown = 0;

        public MagicGolemMeleeAttackGoal(Magic_Golem golem, double speedModifier, boolean followingTargetEvenIfNotSeen) {
            this.golem = golem;
            this.speedModifier = speedModifier;
            this.setFlags(java.util.EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.golem.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void start() {
            this.attackCooldown = 5;
        }

        @Override
        public void tick() {
            LivingEntity target = this.golem.getTarget();
            if (target == null) return;

            // ターゲットの方向を向く
            this.golem.getLookControl().setLookAt(target, 30.0F, 30.0F);

            double distanceSq = this.golem.distanceToSqr(target.getX(), target.getY(), target.getZ());

            if (attackCooldown > 0) {
                attackCooldown--;
            }

            if (!this.golem.isAttackingExternal()) {
                if (distanceSq <= 12.25D && attackCooldown <= 0) {
                    executeRandomAttack();
                } else {
                    this.golem.getNavigation().moveTo(target, this.speedModifier);
                }
            }
        }

        private void executeRandomAttack() {
            float rand = this.golem.getRandom().nextFloat();

            if (rand < 0.35F) {
                this.golem.startNormalAttack();
                attackCooldown = 5;
            } else if (rand < 0.70F) {
                this.golem.startSwingAttack();
                attackCooldown = 10; // (2.0秒)
            } else if (rand < 0.90F) {
                this.golem.startSummonAttack();
                attackCooldown = 20; // (3.0秒)
            } else {
                this.golem.startChargeAttack();
                attackCooldown = 60; // (4.0秒)
            }
        }

        @Override
        public void stop() {
            this.golem.getNavigation().stop();
        }
    }
    static class MagicGolemMoveToRaidGoal<T extends Magic_Golem> extends Goal {
        private final T mob;

        public MagicGolemMoveToRaidGoal(T pMob) {
            this.mob = pMob;
            this.setFlags(java.util.EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            // 襲撃中（hasActiveRaid）であり、かつターゲットがいない場合に襲撃中心地へ向かう
            return this.mob.hasActiveRaid() && this.mob.getTarget() == null;
        }

        @Override
        public void tick() {
            if (this.mob.hasActiveRaid()) {
                net.minecraft.world.entity.raid.Raid raid = this.mob.getCurrentRaid();
                if (raid != null) {
                    // 襲撃の中心地へ移動を指示
                    this.mob.getNavigation().moveTo(raid.getCenter().getX(), raid.getCenter().getY(), raid.getCenter().getZ(), 1.0D);
                }
            }
        }
    }
}

