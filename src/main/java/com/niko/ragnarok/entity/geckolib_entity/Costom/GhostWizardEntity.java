package com.niko.ragnarok.entity.geckolib_entity.Costom;

import com.niko.ragnarok.entity.Projectile.BlueFireballEntity;
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

    // ── 死亡演出 ──
    private static final EntityDataAccessor<Boolean> IS_DYING =
            SynchedEntityData.defineId(GhostWizardEntity.class, EntityDataSerializers.BOOLEAN);
    private int customDeathTime = 0;
    private static final int DEATH_DURATION = 30;

    // アニメーションコントローラーからGoalの状態を参照するための保持
    private WizardAttackGoal wizardAttackGoalRef;

    public GhostWizardEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new RkSmoothMoveControl(this, 15.0F);
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
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 12.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this, Player.class, 10, true, false,
                livingEntity -> !(livingEntity instanceof Player player
                        && (player.isCreative() || player.isSpectator()))
        ));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ATTACK_STATE, 0);
        this.entityData.define(IS_DYING, false);
    }

    public int getAttackState() {
        return this.entityData.get(ATTACK_STATE);
    }

    public void setAttackState(int state) {
        this.entityData.set(ATTACK_STATE, state);
    }

    public boolean isActuallyDying() {
        return this.entityData.get(IS_DYING);
    }

    private void setDying(boolean dying) {
        this.entityData.set(IS_DYING, dying);
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
    protected void dropAllDeathLoot(DamageSource damageSource) {
        // 何もしない。ドロップは死亡演出終了時（aiStep側）に一本化する。
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("IsDying", this.isActuallyDying());
        tag.putInt("CustomDeathTime", this.customDeathTime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("IsDying")) {
            this.setDying(tag.getBoolean("IsDying"));
        }
        this.customDeathTime = tag.getInt("CustomDeathTime");
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.isActuallyDying()) {
            this.customDeathTime++;
            this.setDeltaMovement(Vec3.ZERO);
            this.hurtTime = this.hurtDuration;

            if (this.customDeathTime % 4 == 0 && this.level() instanceof ServerLevel sl) {
                sl.sendParticles(
                        ParticleTypes.LARGE_SMOKE,
                        this.getX() + (this.random.nextDouble() - 0.5D) * this.getBbWidth(),
                        this.getY() + this.random.nextDouble() * this.getBbHeight(),
                        this.getZ() + (this.random.nextDouble() - 0.5D) * this.getBbWidth(),
                        2, 0.1D, 0.1D, 0.1D, 0.02D
                );
            }

            if (this.customDeathTime >= DEATH_DURATION) {
                if (!this.level().isClientSide) {
                    if (this.level() instanceof ServerLevel sl) {
                        sl.sendParticles(
                                ParticleTypes.LARGE_SMOKE,
                                this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ(),
                                25,
                                this.getBbWidth() * 0.5D, this.getBbHeight() * 0.5D, this.getBbWidth() * 0.5D,
                                0.05D
                        );
                    }
                    this.dropFromLootTable(this.damageSources().generic(), true);
                    this.remove(RemovalReason.KILLED);
                }
            }
        }
    }

    @Override
    public boolean causeFallDamage(float d, float m, DamageSource s) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.SKELETON_AMBIENT; }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.SKELETON_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.SKELETON_DEATH; }

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
        if (this.getAttackState() > 0) {
            // 攻撃中はベースのidle/walkを完全に停止させ、action_controllerの競合を防ぐ
            return PlayState.STOP;
        }
        if (state.isMoving()) {
            return state.setAndContinue(RawAnimation.begin().thenLoop("walk"));
        }
        return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
    }

    private PlayState actionAnimPredicate(AnimationState<GhostWizardEntity> state) {
        int attackPhase = this.getAttackState();

        return switch (attackPhase) {
            // フェーズ1と2をまとめ、開始モーションの後に自動でループモーションへ繋げる
            case 1, 2 -> state.setAndContinue(RawAnimation.begin()
                    .thenPlay("magic_attack_start")
                    .thenLoop("magic_attack_loop"));
            case 3 -> state.setAndContinue(RawAnimation.begin().thenPlay("magic_attack_end_1"));
            case 4 -> state.setAndContinue(RawAnimation.begin().thenPlay("magic_attack_end_2"));
            default -> PlayState.STOP;
        };
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ──────────────────────────────────────────
    // 攻撃Goal（Gradius方式：移動・詠唱・攻撃を一括処理）
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

        private int attackType = 1; // 1=火の玉, 2=火柱
        private boolean soundPlayed = false;
        private boolean effectSpawned = false; // 火柱 or 火の玉、どちらも共通で使う発生済みフラグ

        private static final double CAST_RANGE_SQ = 144.0D; // 12ブロック以内で詠唱開始
        private static final int COOLDOWN_TICKS = 30;

        // タイムライン（tick()側で attackTimer++ してから呼ばれるので 1 始まり）
        private static final int START_END = 15;                 // magic_attack_start: 1〜15
        private static final int LOOP_START = 16;                // magic_attack_loop開始: 16
        private static final int LOOP_END = 30;                  // magic_attack_loop : 16〜30
        private static final int FIREBALL_SPAWN_TICK = 45;
        private static final int FIREBALL_FINISH_TICK = 55;
        private static final int PILLAR_SPAWN_TICK = 45;
        private static final int PILLAR_FINISH_TICK = 55;

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
            if (this.mob.isActuallyDying()) return false;
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

            // クールダウン中は接近のみ
            if (cooldown > 0) {
                cooldown--;
                mob.getNavigation().moveTo(t, speed);
                return;
            }

            // 詠唱・攻撃実行中
            if (mob.getAttackState() > 0) {
                attackTimer++;
                mob.getNavigation().stop();
                executeAttack(t);
                return;
            }

            // 行動選択：詠唱射程内なら詠唱開始、外なら接近
            double distSq = mob.distanceToSqr(t);
            if (distSq <= CAST_RANGE_SQ) {
                mob.getNavigation().stop();
                startAttack(t);
            } else {
                mob.getNavigation().moveTo(t, speed);
            }
        }

        private void startAttack(LivingEntity target) {
            this.attackTimer = 0;
            this.attackType = mob.getRandom().nextInt(2) + 1; // 1 or 2
            this.soundPlayed = false;
            this.effectSpawned = false;
            this.lockedTarget = target;
            mob.setAttackState(1);
            this.forceFinishAttack = true;
        }

        private void executeAttack(LivingEntity target) {
            // 現在のタイマーに応じたアニメーションフェーズを取得し同期
            mob.setAttackState(getAnimPhase(attackTimer));

            // 詠唱開始（1tick目）：エンチャントテーブルの音
            if (attackTimer == 1) {
                mob.playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 1.0F, 1.0F);
            }

            // ループ開始（16tick目）：エヴォーカーの詠唱音
            if (attackTimer == LOOP_START) {
                mob.playSound(SoundEvents.EVOKER_CAST_SPELL, 1.5F, 0.6F);

                // 火柱攻撃(attackType == 2)の場合は、ここでターゲットの足元座標をロックする
                if (attackType == 2) {
                    this.lockX = target.getX();
                    this.lockY = target.getY();
                    this.lockZ = target.getZ();
                }
            }

            if (attackType == 1) {
                executeFireballAttack(target);
            } else {
                executePillarAttack(target);
            }
        }

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
            // 音が鳴ってから火柱が出るまでの間（16〜44tick）、ロックした座標に予告円を描画し続ける
            if (attackTimer >= LOOP_START && attackTimer < PILLAR_SPAWN_TICK) {
                if (mob.level() instanceof ServerLevel sl) {
                    double radius = 1.0; // 予告円の半径
                    for (int i = 0; i < 4; i++) {
                        double angle = mob.random.nextDouble() * Math.PI * 2;
                        double px = this.lockX + Math.cos(angle) * radius;
                        double pz = this.lockZ + Math.sin(angle) * radius;
                        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, this.lockY + 0.1, pz, 1, 0, 0, 0, 0);
                    }
                }
            }

            if (attackTimer == PILLAR_SPAWN_TICK && !effectSpawned) {
                effectSpawned = true;
                spawnFirePillar(); // 引数なしのメソッドを呼ぶ
            }
            if (attackTimer >= PILLAR_FINISH_TICK) {
                finishAttack(COOLDOWN_TICKS);
            }
        }

        private void finishAttack(int cd) {
            attackTimer = 0;
            cooldown = cd;
            mob.setAttackState(0);
            forceFinishAttack = false;
            soundPlayed = false;
            effectSpawned = false;
        }

        /** アニメーションコントローラー用：現在のattackTimerからどのフェーズを再生すべきか返す */
        int getAnimPhase(int t) {
            if (t <= START_END) return 1;               // magic_attack_start
            if (t <= LOOP_END) return 2;                 // magic_attack_loop
            if (attackType == 1) return 3;               // magic_attck_end_1
            // 火柱側：LOOP_END〜PILLAR_SPAWN_TICKの間はloopを保持し、そこから先はend_2
            if (t <= PILLAR_SPAWN_TICK) return 2;
            return 4;                                    // magic_attack_end_2
        }

        // ──────────────────────────────────────
        // 火の玉
        // ──────────────────────────────────────
        private void spawnFireball(LivingEntity target) {
            BlueFireballEntity fireball = new BlueFireballEntity(mob.level(), mob, target);
            mob.level().addFreshEntity(fireball);
            mob.playSound(SoundEvents.BLAZE_SHOOT, 1.2F, 0.8F);
        }

        // ──────────────────────────────────────
        // 火柱（ターゲットの足元に1本）
        // ──────────────────────────────────────
// 引数の target を消し、ロックした座標 (lockX, lockY, lockZ) を使うように修正する
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
                    ent -> ent != mob && ent.isAlive() && ent instanceof Player
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
    }
}
