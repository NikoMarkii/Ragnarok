package com.niko.ragnarok.entity.geckolib_entity.Costom.Boss;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.client.gui.bossbar.ICustomBossBar;
import com.niko.ragnarok.entity.Boss_Monster;
import com.niko.ragnarok.entity.Projectile.DinocampusBubbleEntity;
import com.niko.ragnarok.entity.others.RkCombatUtil;
import com.niko.ragnarok.entity.others.RkSmoothMoveControl;
import com.niko.ragnarok.network.RagnarokNetwork;
import com.niko.ragnarok.network.ScreenShakePacket;
import com.niko.ragnarok.sound.RagnarokSoundEvents;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

public class DinocampusEntity extends Boss_Monster implements GeoEntity, ICustomBossBar {
    private static final EntityDataAccessor<Integer> ATTACK_STATE =
            SynchedEntityData.defineId(DinocampusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_DYING =
            SynchedEntityData.defineId(DinocampusEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int DEATH_DURATION = 35;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int previousAttackState;
    private int customDeathTime;

    public DinocampusEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 350;
        this.moveControl = new RkSmoothMoveControl(this, 8.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 450.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D) // 少し素早く
                .add(Attributes.ATTACK_DAMAGE, 28.0D) // ネザライト相当を貫くダメージ (13 -> 28)
                .add(Attributes.FOLLOW_RANGE, 100.0D)
                .add(Attributes.ARMOR, 15.0D) // 防御力も強化 (10 -> 15)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
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

    public boolean isPhase2() {
        return this.getHealth() <= this.getMaxHealth() * 0.5F;
    }

    // CHARGE_START(10)/CHARGE_LOOP(11)/CHARGE_END(12)。
    // Goal内部のprivate定数を直接参照できないアニメーションモデル側からも
    // 「突進中かどうか」を判定できるようにするための公開ヘルパー。
    public boolean isCharging() {
        int state = this.getAttackState();
        return state == 10 || state == 11 || state == 12;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new DinocampusAttackGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 14.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this, Player.class, 10, true, false,
                living -> !(living instanceof Player player && (player.isCreative() || player.isSpectator()))));
    }

    @Override
    public void die(DamageSource damageSource) {
        if (!this.level().isClientSide && !this.isActuallyDying()) {
            super.die(damageSource);
            this.setDying(true);
            this.customDeathTime = 0;
            this.setAttackState(0);
        }
    }

    @Override
    protected void tickDeath() {
    }

    @Override
    protected void dropAllDeathLoot(DamageSource damageSource) {
    }

    @Override
    public boolean isDeadOrDying() {
        return this.isActuallyDying() || super.isDeadOrDying();
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
        this.setDying(tag.getBoolean("IsDying"));
        this.customDeathTime = tag.getInt("CustomDeathTime");
    }
    private void sendScreenShake(float intensity, int duration) {
        if (this.level().isClientSide()) return;
        if (!(this.level() instanceof ServerLevel sl)) return;

        // 範囲内のプレイヤーにパケット送信
        for (net.minecraft.server.level.ServerPlayer player :
                sl.getPlayers(p -> p.distanceToSqr(this) < 64 * 64)) {
            RagnarokNetwork.CHANNEL.sendTo(
                    new ScreenShakePacket(intensity, duration),
                    player.connection.connection,
                    net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
            );
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.isActuallyDying()) {
            return;
        }

        this.customDeathTime++;
        this.setDeltaMovement(Vec3.ZERO);
        this.hurtTime = this.hurtDuration;

        if (this.level() instanceof ServerLevel sl && this.customDeathTime % 4 == 0) {
            sl.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ(),
                    12, this.getBbWidth() * 0.5D, this.getBbHeight() * 0.5D, this.getBbWidth() * 0.5D, 0.05D);
        }

        if (this.customDeathTime >= DEATH_DURATION && !this.level().isClientSide) {
            this.dropFromLootTable(this.damageSources().generic(), true);
            this.remove(RemovalReason.KILLED);
        }
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return RagnarokSoundEvents.TLEX_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(RagnarokSoundEvents.TLEX_STEP.get(), 1.0F, 0.8F);
    }

    /**
     * dinocampus.animation.json の bubble_attack1 / bubble_attack2 / bubble_attack_loop
     * それぞれで head・bone ボーンにかかる回転(・boneの位置ズレ)キーフレームを合成し、
     * 実際に泡を撃つ各tickでの mouth ボーンのローカル座標（モデル原点＝足元基準、単位:ブロック）
     * をあらかじめ計算した実測値。値は Python で行列計算して求めた。
     * （GeoBone#getWorldPosition() はクライアント描画時にしか正しい値を返さないため、
     * 　サーバー側で動くこの攻撃ロジックからは使えない）
     */
    private Vec3 getMouthLocalOffset(int attackState, int timer) {
        return switch (attackState) {
            // BUBBLE_SHOT（bubble_attack1）t=25tick時点
            case 5 -> new Vec3(0.0D, 1.61D, -4.87D);
            // BUBBLE_DOUBLE_RED（bubble_attack2）t=25/35tick、どちらもほぼ同じ姿勢
            case 6 -> new Vec3(0.0D, 1.26D, -4.59D);
            // BUBBLE_STREAM_LOOP（bubble_attack_loop）7tickごとに発射されるタイミングそれぞれ
            case 3 -> switch (timer) {
                case 1 -> new Vec3(-0.25D, 1.58D, -4.85D);
                case 8 -> new Vec3(-1.84D, 1.39D, -4.22D);
                case 15 -> new Vec3(-0.64D, 1.45D, -4.78D);
                case 22 -> new Vec3(1.08D, 1.43D, -4.68D);
                default -> new Vec3(1.48D, 1.45D, -4.51D); // 29tick、および想定外の値のフォールバック
            };
            // それ以外（静止姿勢）
            default -> new Vec3(0.0D, 2.86D, -4.5D);
        };
    }

    private Vec3 getMouthPosition(int attackState, int timer) {
        Vec3 local = getMouthLocalOffset(attackState, timer);

        // 水平面のforward/right基底ベクトルを作り、ローカルのX(左右)・Z(前後)成分を
        // 現在の向きに合わせて回転させる。forwardは既存コードで実績のあるgetLookAngle()基準。
        Vec3 forward = this.getLookAngle();
        forward = new Vec3(forward.x, 0.0D, forward.z).normalize();
        Vec3 right = new Vec3(forward.z, 0.0D, -forward.x); // forwardを90度回転させただけの垂直ベクトル

        double forwardDist = -local.z; // ローカルZは前方が負の値なので符号を反転
        double sideDist = local.x;

        return this.position()
                .add(0.0D, local.y, 0.0D)
                .add(forward.scale(forwardDist))
                .add(right.scale(sideDist));
    }

    // 攻撃状態・タイマー情報が無い呼び出し元向けの簡易版（静止姿勢を返す）
    private Vec3 getMouthPosition() {
        return getMouthPosition(0, 0);
    }

    private void shootBubble(LivingEntity target, int variant, double yawOffsetDegrees, int timer) {
        if (this.level().isClientSide || target == null) {
            return;
        }

        Vec3 mouth = this.getMouthPosition(this.getAttackState(), timer);
        Vec3 toTarget = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D)
                .subtract(mouth)
                .normalize();
        Vec3 direction = rotateHorizontal(toTarget, yawOffsetDegrees).normalize();
        double speed = variant == DinocampusBubbleEntity.YELLOW ? 0.35D : 0.46D;

        DinocampusBubbleEntity bubble = new DinocampusBubbleEntity(
                this.level(), this, target, variant, mouth, direction.scale(speed));
        this.level().addFreshEntity(bubble);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundSource.HOSTILE, 1.0F, 0.8F);
    }

    private static Vec3 rotateHorizontal(Vec3 vector, double degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(vector.x * cos - vector.z * sin, vector.y, vector.x * sin + vector.z * cos);
    }

    private void doMeleeHit(double forward, double inflate, float damageMultiplier, double knockback, double yKnockback) {
        Vec3 look = this.getLookAngle();
        AABB box = this.getBoundingBox()
                .move(look.x * forward, 0.0D, look.z * forward)
                .inflate(inflate, 1.6D, inflate);

        for (LivingEntity entity : this.level().getEntitiesOfClass(LivingEntity.class, box,
                living -> living != this && living.isAlive() && !(living instanceof DinocampusEntity))) {
            entity.hurt(this.damageSources().mobAttack(this),
                    (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * damageMultiplier);

            Vec3 kb = entity.position().subtract(this.position()).normalize().scale(knockback);
            entity.setDeltaMovement(kb.x, yKnockback, kb.z);
            entity.hurtMarked = true;
        }
    }

    private void spawnCircularBlockLift() {
        if (!(this.level() instanceof ServerLevel sl)) {
            return;
        }

        BlockPos center = this.blockPosition();
        for (int radius = 2; radius <= 7; radius++) {
            spawnBlockRing(sl, center, radius);
        }

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.4F, 0.65F);
    }

    private void spawnBlockRing(ServerLevel level, BlockPos center, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double dist = Math.sqrt(x * x + z * z);
                if (dist < radius - 0.5D || dist > radius + 0.5D) {
                    continue;
                }

                BlockPos targetPos = center.offset(x, -1, z);
                BlockState state = level.getBlockState(targetPos);
                if (state.isAir() || state.getDestroySpeed(level, targetPos) < 0) {
                    continue;
                }

                FallingBlockEntity block = new FallingBlockEntity(net.minecraft.world.entity.EntityType.FALLING_BLOCK, level);
                block.setPos(targetPos.getX() + 0.5D, targetPos.getY() + 1.0D, targetPos.getZ() + 0.5D);

                CompoundTag tag = new CompoundTag();
                block.saveWithoutId(tag);
                tag.put("BlockState", net.minecraft.nbt.NbtUtils.writeBlockState(state));
                tag.putInt("Time", 580);
                tag.putBoolean("DropItem", false);
                tag.putBoolean("NoPhysics", true);
                block.load(tag);

                block.noPhysics = true;
                block.setDeltaMovement(0.0D, 0.28D + radius * 0.03D, 0.0D);
                level.addFreshEntity(block);
            }
        }
    }

    private void spawnYellowSpiralWave() {
        LivingEntity target = this.getTarget();
        Vec3 center = this.position().add(this.getLookAngle().scale(2.0D));
        for (int i = 0; i < 20; i++) {
            double angle = i * 0.62D;
            double speed = 0.18D + i * 0.012D;
            Vec3 direction = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle)).scale(speed);
            DinocampusBubbleEntity bubble = new DinocampusBubbleEntity(
                    this.level(), this, target, DinocampusBubbleEntity.YELLOW,
                    center.add(direction.normalize().scale(0.5D)), direction);
            this.level().addFreshEntity(bubble);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "base_controller", 5, state -> {
            if (this.isDeadOrDying()) {
                return PlayState.STOP;
            }
            if (this.getAttackState() > 0) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
            }
            if (state.isMoving()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));

        controllers.add(new AnimationController<>(this, "action_controller", 3, state -> {
            if (this.isActuallyDying()) {
                return state.setAndContinue(RawAnimation.begin().thenPlayAndHold("death"));
            }

            int attackState = this.getAttackState();
            if (attackState <= 0) {
                this.previousAttackState = 0;
                return PlayState.STOP;
            }

            if (this.previousAttackState != attackState) {
                state.getController().forceAnimationReset();
                this.previousAttackState = attackState;
            }

            return state.setAndContinue(RawAnimation.begin().thenPlay(getAnimationName(attackState)));
        }));
    }

    private static String getAnimationName(int state) {
        return switch (state) {
            case 1 -> "attack1";
            case 2 -> "bubble_attack_start";
            case 3 -> "bubble_attack_loop";
            case 4 -> "bubble_attack_end";
            case 5 -> "bubble_attack1";
            case 6 -> "bubble_attack2";
            case 7 -> "stomp_attack";
            case 8 -> "tail_attack1";
            case 9 -> "tail_attack2";
            case 10 -> "charge_attack_start";
            case 11 -> "charge_attack_loop";
            case 12 -> "charge_attack_end";
            default -> "idle";
        };
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public ResourceLocation getBossBarBaseTexture() {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID,
                "textures/gui/dinocampus_bossbar/dinocampus_boss_bar_base1.png");
    }

    @Override
    public ResourceLocation getBossBarOverlayTexture() {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID,
                "textures/gui/dinocampus_bossbar/dinocampus_boss_bar_overlay1.png");
    }

    @Override
    public float getBossProgress() {
        return Mth.clamp(this.getHealth() / this.getMaxHealth(), 0.0F, 1.0F);
    }

    @Override
    public int getFrameWidth() {
        return 128;
    }

    @Override
    public int getFrameHeight() {
        return 30;
    }

    @Override
    public int getFrameOffsetX() {
        return -4;
    }

    @Override
    public int getFrameOffsetY() {
        return -12;
    }

    @Override
    public boolean shouldShowBossBar() {
        return this.isAlive() && !this.isActuallyDying();
    }

    static class DinocampusAttackGoal extends Goal {
        private static final int ATTACK_1 = 1;
        private static final int BUBBLE_STREAM_START = 2;
        private static final int BUBBLE_STREAM_LOOP = 3;
        private static final int BUBBLE_STREAM_END = 4;
        private static final int BUBBLE_SHOT = 5;
        private static final int BUBBLE_DOUBLE_RED = 6;
        private static final int STOMP = 7;
        private static final int TAIL_KNOCKBACK = 8;
        private static final int TAIL_SPIRAL = 9;
        private static final int CHARGE_START = 10;
        private static final int CHARGE_LOOP = 11;
        private static final int CHARGE_END = 12;

        private final DinocampusEntity mob;
        private final double speed;
        private LivingEntity target;
        private int timer;
        private int cooldown;
        private boolean hitDone;
        private Vec3 chargeDirection = Vec3.ZERO;
        private int streamShots;
        private final List<FallingBlockEntity> activeBlocks = new ArrayList<>();
        private boolean forceFinishAttack;
        private int noTargetFreezeTicks;

        DinocampusAttackGoal(DinocampusEntity mob, double speed) {
            this.mob = mob;
            this.speed = speed;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.mob.isActuallyDying()) return false;
            // 攻撃中はターゲット不在でもGoalを離さない
            if (this.forceFinishAttack) return true;
            LivingEntity living = this.mob.getTarget();
            return living != null && living.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.target = this.mob.getTarget();
            this.timer = 0;
            this.cooldown = 20;
            this.hitDone = false;
        }

        @Override
        public void stop() {
            this.mob.setAttackState(0);
            this.timer = 0;
            this.hitDone = false;
            this.streamShots = 0;
            this.activeBlocks.clear();
            this.mob.getNavigation().stop();
        }

        @Override
        public void tick() {
            tickBlockDamage();

            LivingEntity t = this.mob.getTarget();

            LivingEntity currentTarget = this.mob.getTarget();
            if (currentTarget == null || !currentTarget.isAlive()) {
                if (this.mob.getAttackState() == CHARGE_LOOP || this.mob.getAttackState() == CHARGE_START) {
                    // 突進中などはターゲット不在で即中断
                    this.mob.setAttackState(0);
                    this.forceFinishAttack = false;
                    this.mob.getNavigation().stop();
                    RkCombatUtil.faceTarget(mob, t);
                    return;
                }

                if (!this.forceFinishAttack) {
                    this.mob.setAttackState(0);
                    this.mob.getNavigation().stop();
                    return;
                }

                // 攻撃アニメーション中にターゲットが消えた場合、アニメーションが終わるまで待機
                this.mob.getNavigation().stop();
                this.noTargetFreezeTicks++;
                if (this.noTargetFreezeTicks > 30) {
                    this.forceFinishAttack = false;
                    this.mob.setAttackState(0);
                    this.noTargetFreezeTicks = 0;
                }
                return;
            }

            this.noTargetFreezeTicks = 0;
            this.target = currentTarget;

            this.mob.getLookControl().setLookAt(this.target, 30.0F, 30.0F);

            if (this.cooldown > 0 && this.mob.getAttackState() == 0) {
                this.cooldown--;
                this.mob.getNavigation().moveTo(this.target, this.speed);
                return;
            }

            if (this.mob.getAttackState() == 0) {
                if (this.mob.distanceToSqr(this.target) <= 49.0D) {
                    startAttack();
                } else if (this.mob.random.nextInt(80) == 0) {
                    startRangedAttack();
                } else {
                    this.mob.getNavigation().moveTo(this.target, this.speed);
                }
                return;
            }

            this.timer++;
            faceTarget();
            executeAttack();
        }

        private void startAttack() {
            int roll = this.mob.random.nextInt(7);
            int state = switch (roll) {
                case 0 -> ATTACK_1;
                case 1 -> BUBBLE_SHOT;
                case 2 -> BUBBLE_DOUBLE_RED;
                case 3 -> STOMP;
                case 4 -> TAIL_KNOCKBACK;
                case 5 -> TAIL_SPIRAL;
                default -> CHARGE_START;
            };
            beginState(state);
        }

        private void startRangedAttack() {
            beginState(this.mob.random.nextBoolean() ? BUBBLE_STREAM_START : BUBBLE_SHOT);
        }

        private void beginState(int state) {
            this.mob.setAttackState(state);
            this.timer = 0;
            this.hitDone = false;
            this.streamShots = 0;
            this.forceFinishAttack = true; // ★攻撃開始時にフラグを立てる
            this.mob.getNavigation().stop();
        }

        private void executeAttack() {
            switch (this.mob.getAttackState()) {
                case ATTACK_1 -> tickAttack1();
                case BUBBLE_STREAM_START -> tickBubbleStreamStart();
                case BUBBLE_STREAM_LOOP -> tickBubbleStreamLoop();
                case BUBBLE_STREAM_END -> tickBubbleStreamEnd();
                case BUBBLE_SHOT -> tickBubbleShot();
                case BUBBLE_DOUBLE_RED -> tickBubbleDoubleRed();
                case STOMP -> tickStomp();
                case TAIL_KNOCKBACK -> tickTailKnockback();
                case TAIL_SPIRAL -> tickTailSpiral();
                case CHARGE_START -> tickChargeStart();
                case CHARGE_LOOP -> tickChargeLoop();
                case CHARGE_END -> tickChargeEnd();
                default -> finish(20);
            }
        }

        private void tickAttack1() {
            if (!this.hitDone && this.timer >= 25) {
                this.hitDone = true;
                this.mob.doMeleeHit(2.5D, 2.2D, 1.0F, 1.1D, 0.35D);
            }
            if (this.timer >= 45) {
                finish(25);
            }
        }

        private void tickBubbleStreamStart() {
            if (this.timer >= 25) {
                beginState(BUBBLE_STREAM_LOOP);
            }
        }

        private void tickBubbleStreamLoop() {
            if (this.timer % 7 == 1) {
                int variant = DinocampusBubbleEntity.BLUE;
                if (this.mob.isPhase2()) {
                    variant = (this.streamShots++ % 2 == 0)
                            ? DinocampusBubbleEntity.BLUE
                            : DinocampusBubbleEntity.RED;
                }
                this.mob.shootBubble(this.target, variant, 0.0D, this.timer);
            }
            if (this.timer >= 35) {
                beginState(BUBBLE_STREAM_END);
            }
        }

        private void tickBubbleStreamEnd() {
            if (this.timer >= 10) {
                finish(35);
            }
        }

        private void tickBubbleShot() {
            if (!this.hitDone && this.timer >= 25) {
                this.hitDone = true;
                int variant = this.mob.random.nextBoolean()
                        ? DinocampusBubbleEntity.BLUE
                        : DinocampusBubbleEntity.RED;
                if (this.mob.isPhase2()) {
                    this.mob.shootBubble(this.target, variant, -15.0D, this.timer);
                    this.mob.shootBubble(this.target, variant, 0.0D, this.timer);
                    this.mob.shootBubble(this.target, variant, 15.0D, this.timer);
                } else {
                    this.mob.shootBubble(this.target, variant, 0.0D, this.timer);
                }
            }
            if (this.timer >= 45) {
                finish(30);
            }
        }

        private void tickBubbleDoubleRed() {
            if (this.timer == 25 || this.timer == 35) {
                if (this.mob.isPhase2()) {
                    this.mob.shootBubble(this.target, DinocampusBubbleEntity.RED, -8.0D, this.timer);
                    this.mob.shootBubble(this.target, DinocampusBubbleEntity.RED, 8.0D, this.timer);
                } else {
                    this.mob.shootBubble(this.target, DinocampusBubbleEntity.RED, 0.0D, this.timer);
                }
            }
            // ★変更: こちらも同様に延長 (例: 45 -> 70)
            if (this.timer >= 45) {
                finish(35);
            }
        }

        private void tickStomp() {
            if (!this.hitDone && this.timer >= 35) {
                this.hitDone = true;
                this.mob.doMeleeHit(1.0D, 4.0D, 1.15F, 1.6D, 0.65D);
                mob.sendScreenShake(1.5F, 15);
                this.mob.playSound(SoundEvents.GENERIC_EXPLODE, 1.5F, 0.8F);
                this.mob.spawnCircularBlockLift();
            }
            if (this.timer >= 55) {
                finish(40);
            }
        }

        private void tickTailKnockback() {
            if (!this.hitDone && this.timer >= 25) {
                this.hitDone = true;
                this.mob.doMeleeHit(-1.5D, 4.0D, 0.95F, 2.8D, 0.75D);
            }
            if (this.timer >= 55) {
                finish(35);
            }
        }

        private void tickTailSpiral() {
            if (this.timer == 35 || this.timer == 55) {
                this.mob.spawnYellowSpiralWave();
                this.mob.playSound(SoundEvents.GENERIC_EXPLODE, 1.5F, 0.8F);
                mob.sendScreenShake(1.5F, 15);
            }
            if (this.timer >= 80) {
                finish(45);
            }
        }

        private void tickChargeStart() {
            if (this.timer == 1) {
                this.chargeDirection = this.target.position().subtract(this.mob.position());
                this.chargeDirection = new Vec3(this.chargeDirection.x, 0.0D, this.chargeDirection.z).normalize();
                if (this.chargeDirection.lengthSqr() < 0.001D) {
                    this.chargeDirection = this.mob.getLookAngle();
                }
            }
            if (this.timer >= 25) {
                beginState(CHARGE_LOOP);
            }
        }

        private void tickChargeLoop() {
            this.mob.setDeltaMovement(this.chargeDirection.scale(0.85D));
            this.mob.doMeleeHit(2.0D, 2.0D, 1.1F, 2.0D, 0.45D);

            if (this.mob.horizontalCollision || this.timer >= 100) {
                beginState(CHARGE_END);
            }
        }

        private void tickChargeEnd() {
            this.mob.setDeltaMovement(Vec3.ZERO);
            if (this.timer >= 10) {
                finish(45);
            }
        }

        private void faceTarget() {
            int state = this.mob.getAttackState();

            if (state == CHARGE_START || state == CHARGE_LOOP || state == CHARGE_END) {
                // 突進中は「ターゲット」ではなく「実際に決定した突進方向(chargeDirection)」を
                // 向かせる。ここを完全にスキップすると、突進開始直前の古い向きのまま固まり、
                // 実際に移動している方向とズレて後ろ向き・横向きに歩いているように見えてしまう。
                // chargeDirectionはCHARGE_START中のtimer==1で1回だけ決定され、それ以降は
                // 変わらないので、首がターゲットを追ってカクつくこともない。
                if (this.chargeDirection.lengthSqr() > 0.0001D) {
                    float chargeYaw = (float) (Math.toDegrees(
                            Math.atan2(this.chargeDirection.z, this.chargeDirection.x)) - 90.0F);
                    this.mob.setYRot(chargeYaw);
                    this.mob.yBodyRot = chargeYaw;
                    this.mob.yHeadRot = chargeYaw;
                }
                return;
            }

            if (this.target == null) {
                return;
            }

            Vec3 lookVec = this.target.position().subtract(this.mob.position());
            float yRot = (float) (Math.toDegrees(Math.atan2(lookVec.z, lookVec.x)) - 90.0F);
            this.mob.setYRot(yRot);
            this.mob.yBodyRot = yRot;
            this.mob.yHeadRot = yRot;
        }

        private void finish(int cooldown) {
            this.mob.setAttackState(0);
            this.timer = 0;
            this.cooldown = cooldown;
            this.hitDone = false;
            this.streamShots = 0;
            this.forceFinishAttack = false; // ★攻撃終了でフラグを下ろす
            this.mob.setDeltaMovement(Vec3.ZERO);
        }

        private void tickBlockDamage() {
            if (this.mob.level().isClientSide()) {
                return;
            }

            Iterator<FallingBlockEntity> iterator = this.activeBlocks.iterator();
            while (iterator.hasNext()) {
                FallingBlockEntity block = iterator.next();
                if (!block.isAlive()) {
                    iterator.remove();
                    continue;
                }

                AABB box = block.getBoundingBox().inflate(0.15D);
                for (LivingEntity living : this.mob.level().getEntitiesOfClass(LivingEntity.class, box,
                        entity -> entity != this.mob && entity.isAlive())) {
                    living.hurt(this.mob.damageSources().mobAttack(this.mob), 8.0F);
                }
            }
        }
    }
}