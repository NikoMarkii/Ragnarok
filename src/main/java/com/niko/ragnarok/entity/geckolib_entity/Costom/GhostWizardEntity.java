package com.niko.ragnarok.entity.geckolib_entity.Costom;

import com.niko.ragnarok.entity.Projectile.BlueFireballEntity;
import com.niko.ragnarok.entity.RagnarokEntities;
import com.niko.ragnarok.entity.others.RkBodyRotationControl;
import com.niko.ragnarok.entity.others.RkSmoothMoveControl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
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
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

public class GhostWizardEntity extends Monster implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // ── 攻撃状態（0=非攻撃, 1=詠唱〜攻撃中） ──
    private static final EntityDataAccessor<Integer> ATTACK_STATE =
            SynchedEntityData.defineId(GhostWizardEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> ATTACK_TYPE =
            SynchedEntityData.defineId(GhostWizardEntity.class, EntityDataSerializers.INT);

    private WizardAttackGoal wizardAttackGoalRef;

    public GhostWizardEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new RkSmoothMoveControl(this, 15.0F);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ATTACK_TYPE, 0);
        this.entityData.define(ATTACK_STATE, 0);
    }

    @Override
    protected net.minecraft.world.entity.ai.control.BodyRotationControl createBodyControl() {
        return new RkBodyRotationControl(this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 45.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 2.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new WizardAttackGoal(this, 1.0D));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 8.0F, 0.6F, 1.0D));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 12.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this, Player.class, 10, true, false,
                livingEntity -> !(livingEntity instanceof Player player
                        && (player.isCreative() || player.isSpectator()))
        ));
    }

    public int getAttackState() {
        return this.entityData.get(ATTACK_STATE);
    }

    public void setAttackState(int state) {
        this.entityData.set(ATTACK_STATE, state);
    }

    public int getAttacktype() {
        return this.entityData.get(ATTACK_TYPE);
    }

    public void setAttackType(int type) {
        this.entityData.set(ATTACK_TYPE, type);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.level().isClientSide && this.getAttackState() > 0) {
            spawnCastingParticles();
        }
    }

    private void spawnCastingParticles() {
        int type = this.getAttacktype();

        double px = this.getX() + (this.random.nextDouble() - 0.5D) * 0.8D;
        double py = this.getY() + 1.2D + (this.random.nextDouble() - 0.5D) * 0.5D; // 胸〜頭の高さ
        double pz = this.getZ() + (this.random.nextDouble() - 0.5D) * 0.8D;

        switch (type) {
            case 1 -> { // 技1：火の玉（青い炎）
                this.level().addParticle(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        px, py, pz,
                        0.0D, 0.02D, 0.0D
                );
            }
            case 2 -> { // 技2：火柱（ソウル＋青い炎で激しく）
                this.level().addParticle(
                        ParticleTypes.SOUL,
                        px, py, pz,
                        (this.random.nextDouble() - 0.5D) * 0.1D, 0.05D, (this.random.nextDouble() - 0.5D) * 0.1D
                );
            }
            case 3 -> { // 技3：直線火柱（怪しげなエンチャント文字＋煙）
                this.level().addParticle(
                        ParticleTypes.ENCHANT,
                        px, py, pz,
                        (this.random.nextDouble() - 0.5D) * 0.5D, (this.random.nextDouble() - 0.5D) * 0.5D, (this.random.nextDouble() - 0.5D) * 0.5D
                );
            }
            case 4 -> { // 技4：召喚（不気味な煙・ポーション粒子）
                this.level().addParticle(
                        ParticleTypes.WITCH,
                        px, py, pz,
                        0.0D, 0.0D, 0.0D
                );
            }
            case 5 -> {
                this.level().addParticle(
                        ParticleTypes.HAPPY_VILLAGER,
                        px, py, pz,
                        0.0D, 0.05D, 0.0D
                );
            }
        }
    }

    @Override
    protected void dropAllDeathLoot(DamageSource damageSource) {
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SKELETON_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        this.playSound(SoundEvents.SKELETON_STEP, 1.0F, 1.0F);
    }

    // ──────────────────────────────────────────
    // アニメーション
    // ──────────────────────────────────────────
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "base_controller", 5, this::baseAnimPredicate));
        controllers.add(new AnimationController<>(this, "action_controller", 0, this::actionAnimPredicate));
    }

    private PlayState baseAnimPredicate(AnimationState<GhostWizardEntity> state) {
        if (this.getAttackState() > 0) return PlayState.STOP;
        if (state.isMoving()) return state.setAndContinue(RawAnimation.begin().thenLoop("walk"));
        return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
    }

    private PlayState actionAnimPredicate(AnimationState<GhostWizardEntity> state) {
        int attackPhase = this.getAttackState();

        return switch (attackPhase) {
            case 1, 2 -> state.setAndContinue(RawAnimation.begin()
                    .thenPlay("magic_attack_start")
                    .thenLoop("magic_attack_loop"));
            case 3 -> state.setAndContinue(RawAnimation.begin().thenPlay("magic_attack_end_1"));
            case 4 -> state.setAndContinue(RawAnimation.begin().thenPlay("magic_attack_end_2"));
            // 召喚用に追加したフェーズ
            case 5 -> state.setAndContinue(RawAnimation.begin().thenPlay("magic_attack_end_2"));

            case 6 -> state.setAndContinue(RawAnimation.begin().thenPlay("magic_attack_end_1"));
            default -> PlayState.STOP;
        };
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ──────────────────────────────────────────
    // 攻撃Goal（エヴォーカー風の距離調整＋召喚機能を追加）
    // ──────────────────────────────────────────
    public static class WizardAttackGoal extends Goal {

        private final GhostWizardEntity mob;
        private final double speed;
        private LivingEntity lockedTarget;

        private int attackTimer = 0;
        private int cooldown = 0;
        private boolean forceFinishAttack = false;
        private int noTargetFreezeTicks = 0;
        private static final int MAX_NO_TARGET_FREEZE_TICKS = 20;

        private int attackType = 1;
        private boolean soundPlayed = false;
        private boolean effectSpawned = false;

        private static final double CAST_RANGE_SQ = 144.0D; // 12ブロック以内で詠唱開始
        private static final double RETREAT_RANGE_SQ = 80.0D; // 6ブロック未満なら逃げる
        private static final int COOLDOWN_TICKS = 30;

        private static final int START_END = 15;
        private static final int LOOP_START = 16;
        private static final int LOOP_END = 30;
        private static final int FIREBALL_SPAWN_TICK = 40;
        private static final int FIREBALL_FINISH_TICK = 55;
        private static final int PILLAR_CLYCLE_SPAWN_TICK = 33;
        private static final int PILLAR_CLYCLE_SPAWN_TICK_2 = 43;
        private static final int PILLAR_SPAWN_TICK = 35;
        private static final int PILLAR_SPAWN_TICK_2 = 45;
        private static final int PILLAR_FINISH_TICK = 55;
        private static final int PILLAR_LINE_PRE_SPAWN_TICK = 20;
        private static final int PILLAR_LINE_SPAWN_TICK = 25;

        private double lockX;
        private double lockY;
        private double lockZ;

        public WizardAttackGoal(GhostWizardEntity mob, double speed) {
            this.mob = mob;
            this.speed = speed;
            this.mob.wizardAttackGoalRef = this;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.forceFinishAttack) return true;
            LivingEntity t = mob.getTarget();
            return t != null && t.isAlive();
        }

        @Override
        public void start() {
            this.attackTimer = 0;
            this.cooldown = 0;
        }

        @Override
        public void stop() {
            mob.setAttackState(0);
            attackTimer = 0;
            forceFinishAttack = false;
            noTargetFreezeTicks = 0;
            mob.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity t = mob.getTarget();

            if (attackTimer >= LOOP_START) {
                if (attackType == 1) {

                }
            }

            if (t == null || !t.isAlive()) {
                if (this.forceFinishAttack) {
                    mob.getNavigation().stop();
                    noTargetFreezeTicks++;
                    if (noTargetFreezeTicks > MAX_NO_TARGET_FREEZE_TICKS) {
                        forceFinishAttack = false;
                        mob.setAttackState(0);
                        noTargetFreezeTicks = 0;
                    }
                    return;
                }
                mob.setAttackState(0);
                return;
            }
            noTargetFreezeTicks = 0;
            this.lockedTarget = t;

            mob.getLookControl().setLookAt(t, 30F, 30F);
            double distSq = mob.distanceToSqr(t);

            // 詠唱・攻撃実行中
            if (mob.getAttackState() > 0) {
                attackTimer++;
                mob.getNavigation().stop();
                executeAttack(t);
                return;
            }

            // クールダウン中はエヴォーカーのように距離を調整する
            if (cooldown > 0) {
                cooldown--;
                adjustPosition(t, distSq);
                return;
            }

            // 行動選択：詠唱射程内なら詠唱開始、外なら接近
            if (distSq <= CAST_RANGE_SQ) {
                mob.getNavigation().stop();
                startAttack(t);
            } else {
                mob.getNavigation().moveTo(t, speed);
            }
        }

        // エヴォーカーらしい距離の引き撃ち処理
        private void adjustPosition(LivingEntity target, double distSq) {
            if (distSq < RETREAT_RANGE_SQ) {
                // 近すぎる場合は逆方向に逃げる
                Vec3 away = mob.position().subtract(target.position()).normalize().scale(5);
                Vec3 targetPos = mob.position().add(away);
                mob.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, speed * 1.5D);
            } else if (distSq > CAST_RANGE_SQ) {
                // 遠すぎる場合は近づく
                mob.getNavigation().moveTo(target, speed);
            } else {
                // 適正距離なら立ち止まる
                mob.getNavigation().stop();
            }
        }

        private void startAttack(LivingEntity target) {
            this.attackTimer = 0;

            // 周囲のGhostEntityの数を数える
            int ghostCount = mob.level().getEntitiesOfClass(GhostEntity.class, mob.getBoundingBox().inflate(16.0)).size();
            float healthRatio = mob.getHealth() / mob.getMaxHealth();

            if (healthRatio <= 0.5F && mob.getRandom().nextBoolean()) {
                this.attackType = 5;
            } else {
                if (ghostCount < 6) {
                    // 6体未満なら 1(火の玉), 2(火柱), 3(直線火柱), 4(召喚) の4つから選ぶ
                    this.attackType = mob.getRandom().nextInt(4) + 1;
                } else {
                    // 3体以上なら 1, 2, 3 (攻撃技のみ) から選ぶ
                    this.attackType = mob.getRandom().nextInt(3) + 1;
                }
            }


                mob.setAttackType(this.attackType);

                this.soundPlayed = false;
                this.effectSpawned = false;
                this.lockedTarget = target;
                mob.setAttackState(1);
                this.forceFinishAttack = true;
        }

        private void executeAttack(LivingEntity target) {
            mob.setAttackState(getAnimPhase(attackTimer));

            if (attackTimer == 1) {
                mob.playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 1.0F, 1.0F);
            }

            if (attackTimer == LOOP_START) {
                mob.playSound(SoundEvents.EVOKER_CAST_SPELL, 1.5F, 0.6F);

            }

            if (attackType == 1) {
                executeFireballAttack(target);
            } else if (attackType == 2) {
                executePillarAttack(target);

            if (attackTimer == PILLAR_CLYCLE_SPAWN_TICK || attackTimer == PILLAR_CLYCLE_SPAWN_TICK_2){
            this.lockX = target.getX();
            this.lockY = target.getY();
            this.lockZ = target.getZ();
            }

            } else if (attackType == 3){
                executePillarLineAttack(target);
                if (attackTimer == PILLAR_LINE_PRE_SPAWN_TICK){
                    this.lockX = target.getX();
                    this.lockY = target.getY();
                    this.lockZ = target.getZ();
                }
            } else if (attackType == 4) {
                executeSummonAttack(target);
            } else if (attackType == 5) {
                executeHealAttack();
            }
        }

        // 既存の火の玉・火柱の処理はそのまま維持
        private void executeFireballAttack(LivingEntity target) {
            if (attackTimer == FIREBALL_SPAWN_TICK && !effectSpawned) {
                effectSpawned = true;
                spawnFireball(target);
            }
            if (attackTimer >= FIREBALL_FINISH_TICK) {
                finishAttack(COOLDOWN_TICKS);
            }
        }

        private void executePillarAttack(LivingEntity target) {
            if (attackTimer == PILLAR_CLYCLE_SPAWN_TICK || attackTimer == PILLAR_CLYCLE_SPAWN_TICK_2) {
                mob.playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 1.2F, 0.8F);

                if(attackTimer == PILLAR_CLYCLE_SPAWN_TICK_2) {
                    effectSpawned = false;
                }

                boolean iswarning1 = (attackTimer >= PILLAR_CLYCLE_SPAWN_TICK && attackTimer < PILLAR_SPAWN_TICK);

                boolean iswarning2 = (attackTimer >= PILLAR_CLYCLE_SPAWN_TICK_2 && attackTimer < PILLAR_SPAWN_TICK_2);

                if (iswarning1 || iswarning2) {
                    if (mob.level() instanceof ServerLevel sl) {
                        double radius = 1.0;
                        for (int i = 0; i < 4; i++) {
                            double angle = mob.random.nextDouble() * Math.PI * 2;
                            double px = this.lockX + Math.cos(angle) * radius;
                            double pz = this.lockZ + Math.sin(angle) * radius;
                            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, this.lockY + 0.1, pz, 1, 0, 0, 0, 0);
                        }
                    }
                }
            }
            if (attackTimer == PILLAR_SPAWN_TICK && !effectSpawned || attackTimer == PILLAR_SPAWN_TICK_2 && !effectSpawned) {
                effectSpawned = true;
                spawnFirePillar();
            }
            if (attackTimer >= PILLAR_FINISH_TICK) {
                finishAttack(COOLDOWN_TICKS);
            }
        }

        // ── 1. 直線火柱攻撃のメインロジック ──
        private void executePillarLineAttack(LivingEntity target) {

            if (attackTimer == PILLAR_LINE_PRE_SPAWN_TICK){
             mob.playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 1.0F,1.5F);
            }

            // 25tick目（PILLAR_LINE_SPAWN_TICK）で一直線に火柱を出す
            if (attackTimer == PILLAR_LINE_SPAWN_TICK && !effectSpawned) {
                effectSpawned = true;
                spawnFirePillarLines(target);
            }

            if (attackTimer >= PILLAR_FINISH_TICK) {
                finishAttack(COOLDOWN_TICKS);
            }
        }

        // 新設：ゴースト召喚攻撃
        private void executeSummonAttack(LivingEntity target) {
            // 詠唱中に専用のパーティクルを出す
            if (attackTimer >= LOOP_START && attackTimer < FIREBALL_SPAWN_TICK) {
                if (mob.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.SOUL, mob.getX(), mob.getY() + 2.0, mob.getZ(), 1, 0.2, 0.2, 0.2, 0.0);
                }
            }

            if (attackTimer == FIREBALL_SPAWN_TICK && !effectSpawned) {
                effectSpawned = true;
                spawnGhosts();
            }
            if (attackTimer >= FIREBALL_FINISH_TICK) {
                finishAttack(COOLDOWN_TICKS);
            }
        }
        private void executeHealAttack() {

            // 発生タイミング（FIREBALL_SPAWN_TICK = 40 tick 目に回復発動）
            if (attackTimer == FIREBALL_SPAWN_TICK && !effectSpawned) {
                effectSpawned = true;

                // 1. 体力を回復（例: 最大HPの35%分を回復）
                float healAmout = mob.getMaxHealth() * 0.35F;
                mob.heal(healAmout);
                mob.playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.5F);

                // 3. サーバー側から周囲に回復の円形エフェクトを飛ばす
                if (mob.level() instanceof ServerLevel sl) {
                    spawnHealParticles(sl);
                }
            }
                if (attackTimer >= FIREBALL_FINISH_TICK) {
                    finishAttack(COOLDOWN_TICKS);
                }
        }
        // ── 2. 直線状に火柱を並べる処理 ──
        private void spawnFirePillarLines(LivingEntity target) {
            if (!(mob.level() instanceof ServerLevel sl)) return;

            // ★ 記憶しておいた lockX, lockZ への方向ベクトルを計算
            Vec3 targetLockPos = new Vec3(this.lockX, this.lockY, this.lockZ);
            Vec3 dir = targetLockPos.subtract(mob.position());

            // Y要素（高低差）を無視して水平方向（X-Z面）の向きだけを取り出す
            dir = new Vec3(dir.x, 0, dir.z).normalize();

            // もし自分とロックオン位置が完全に重なっていた場合の安全策（正面に向ける）
            if (dir.lengthSqr() == 0) {
                dir = mob.getLookAngle();
            }

            int pillarCount = 7;      // 火柱の数（7本）
            double spacing = 1.5;      // 火柱同士の間隔（1.5ブロック刻み）

            for (int i = 1; i <= pillarCount; i++) {
                // 自分の位置から dir（計算した固定方向）に伸ばしていく
                double px = mob.getX() + dir.x * (i * spacing);
                double pz = mob.getZ() + dir.z * (i * spacing);
                double py = mob.getY(); // 地面の高さ

                int delayTicks = i * 2; // 時間差でズズズ…と出していく

                sl.getServer().tell(new net.minecraft.server.TickTask(
                        sl.getServer().getTickCount() + delayTicks,
                        () -> {
                            if (!mob.isAlive()) return;
                            spawnSinglePillarAt(sl, px, py, pz);
                        }
                ));
            }
        }

        // ── 3. 1箇所に火柱を1本だけ発生させる処理（共通化） ──
        private void spawnSinglePillarAt(ServerLevel sl, double x, double baseY, double z) {
            // 爆発演出
            sl.sendParticles(ParticleTypes.EXPLOSION, x, baseY + 0.5, z, 1, 0, 0, 0, 0);

            // 火柱パーティクル
            double height = 4.0;
            for (double y = 0; y < height; y += 0.3) {
                double radius = 0.5;
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, baseY + y, z, 5, radius, 0.05, radius, 0);
                sl.sendParticles(ParticleTypes.SOUL, x, baseY + y, z, 2, radius * 0.7, 0.05, radius * 0.7, 0);
            }

            // ダメージ判定（各火柱の周囲1.0ブロック）
            AABB hitBox = new AABB(x - 1.0, baseY, z - 1.0, x + 1.0, baseY + height, z + 1.0);
            for (LivingEntity e : sl.getEntitiesOfClass(
                    LivingEntity.class, hitBox,
                    ent -> ent != mob && ent.isAlive()
            )) {
                e.invulnerableTime = 0;
                e.hurt(mob.damageSources().mobAttack(mob),
                        (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.2F);

                // 打上ノックバック
                e.setDeltaMovement(e.getDeltaMovement().x, 0.4, e.getDeltaMovement().z);
                e.hurtMarked = true;
            }

            mob.playSound(SoundEvents.TRIDENT_THUNDER, 1.0F, 1.2F);
        }
        private void spawnFireball(LivingEntity target) {
            BlueFireballEntity fireball = new BlueFireballEntity(mob.level(), mob, target);
            mob.level().addFreshEntity(fireball);
            mob.playSound(SoundEvents.BLAZE_SHOOT, 1.2F, 0.8F);
        }

        private void spawnFirePillar() {
            if (!(mob.level() instanceof ServerLevel sl)) return;

            double x = this.lockX;
            double z = this.lockZ;
            double baseY = this.lockY;

            sl.sendParticles(ParticleTypes.EXPLOSION, x, baseY + 0.5, z, 1, 0, 0, 0, 0);

            double height = 5 + mob.random.nextDouble() * 2;
            for (double y = 0; y < height; y += 0.2) {
                double radius = 0.6 + (y / height) * 0.3;
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, baseY + y, z, 8, radius, 0.05, radius, 0);
                sl.sendParticles(ParticleTypes.SOUL, x, baseY + y, z, 4, radius * 0.7, 0.05, radius * 0.7, 0);
            }
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, baseY + height, z, 30, 1.0, 0.3, 1.0, 0.05);

            AABB hitBox = new AABB(x - 1.0, baseY, z - 1.0, x + 1.0, baseY + height, z + 1.0);
            for (LivingEntity e : mob.level().getEntitiesOfClass(
                    LivingEntity.class, hitBox,
                    ent -> ent != mob && ent.isAlive()
            )) {
                e.invulnerableTime = 0;
                e.hurt(mob.damageSources().mobAttack(mob),
                        (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.3F);

                Vec3 kb = e.position().subtract(mob.position()).normalize().scale(1.2);
                e.setDeltaMovement(kb.x, 0.6, kb.z);
                e.hurtMarked = true;
            }

            mob.playSound(SoundEvents.TRIDENT_THUNDER, 1.3F, 0.8F);
        }

        // ──────────────────────────────────────
        // ゴーストの召喚
        // ──────────────────────────────────────
        private void spawnGhosts() {
            if (!(mob.level() instanceof ServerLevel sl)) return;

            mob.playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 1.0F, 1.0F);

            int amount = 1 + mob.getRandom().nextInt(3); // 1〜2体召喚
            for (int i = 0; i < amount; i++) {

                GhostEntity ghost = RagnarokEntities.GHOST.get().create(sl);

                if (ghost != null) {
                    BlockPos spawnPos = mob.blockPosition().offset(
                            -2 + mob.random.nextInt(5),
                            1,
                            -2 + mob.random.nextInt(5)
                    );
                    ghost.moveTo(spawnPos, 0.0F, 0.0F);
                    ghost.finalizeSpawn(sl, sl.getCurrentDifficultyAt(spawnPos), MobSpawnType.MOB_SUMMONED, null, null);
                    ghost.setTarget(mob.getTarget());
                    sl.addFreshEntity(ghost);

                    sl.sendParticles(ParticleTypes.POOF, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), 10, 0.5, 0.5, 0.5, 0.05);
                }
            }
        }

        private void spawnHealParticles(ServerLevel sl) {
            int particleCount = 20;
            double radius = 1.2;

            for(int i = 10; i < particleCount; i++) {
                double angle = i * (2.0 * Math.PI / particleCount);

                double dx = Math.cos(angle) * radius;
                double dz = Math.sin(angle) * radius;

                sl.sendParticles(
                        ParticleTypes.HAPPY_VILLAGER,
                        mob.getX() + dx,
                        mob.getY() + 0.2,
                        mob.getZ() + dz,
                        1,0.0,0.2,0,0.05
                );
                sl.sendParticles(
                        ParticleTypes.INSTANT_EFFECT,
                        mob.getX() + dx,
                        mob.getY() + 1.0,
                        mob.getZ() + dz,
                        1, 0.0, 0.0, 0.0, 0.0
                );
            }
        }

        private void finishAttack(int cd) {
            attackTimer = 0;
            cooldown = cd;
            mob.setAttackState(0);
            mob.setAttackType(0);
            forceFinishAttack = false;
            soundPlayed = false;
            effectSpawned = false;
        }

        int getAnimPhase(int t) {
            if (t <= START_END) return 1;               // magic_attack_start
            if (t <= LOOP_END) return 2;                // magic_attack_loop
            if (attackType == 1) return 3;              // magic_attack_end_1
            // ★ 召喚（attackType == 4）の時、スポーンタイマーを超えたら召喚モーション（5）へ
            if (attackType == 4 && t > FIREBALL_SPAWN_TICK) return 5;

            if (attackType == 5 && t > FIREBALL_SPAWN_TICK) return 6;

            if (t <= PILLAR_SPAWN_TICK) return 2;
            return 4;                                   // magic_attack_end_2
        }
    }
}