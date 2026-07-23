package com.niko.ragnarok.entity.geckolib_entity.Costom;

import com.niko.ragnarok.network.RagnarokNetwork;
import com.niko.ragnarok.network.ScreenShakePacket;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.item.FallingBlockEntity;
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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

/**
 * ガレオス - 中ボス
 * attack1   : 右腕 発生25tick 長さ45tick
 * attack1_2 : 左腕 発生25tick 長さ45tick
 * attack2   : 両腕叩きつけ 発生25tick 長さ45tick
 */
public class GaleosEntity extends Monster implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Integer> ATTACK_STATE =
            SynchedEntityData.defineId(GaleosEntity.class, EntityDataSerializers.INT);

    private static final int ATK_HIT   = 30;
    private static final int ATK_TOTAL = 45;

    private int previousAttackState = 0;

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

    public GaleosEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH,      120.0D)
                .add(Attributes.MOVEMENT_SPEED,    0.24D)
                .add(Attributes.ATTACK_DAMAGE,     10.0D)
                .add(Attributes.FOLLOW_RANGE,      32.0D)
                .add(Attributes.ARMOR,              8.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D);
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
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new GaleosAttackGoal(this, 1.05D));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 12.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.HUSK_AMBIENT; }
    @Override
    protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.HUSK_HURT; }
    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.HUSK_DEATH; }
    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        this.playSound(SoundEvents.RAVAGER_STEP, 1.0F, 1.0F);
    }

    // ──────────────────────────────────────────
    // AttackGoal
    // ──────────────────────────────────────────
    static class GaleosAttackGoal extends Goal {

        private final GaleosEntity mob;
        private final double speed;
        private LivingEntity target;

        private int attackTimer = 0;
        private int cooldown    = 0;
        private int pathRecalcTimer = 0;

        private static final double ATTACK_START_SQ = 25.0D; // 3ブロック以内で攻撃開始

        private final List<ScheduledBlockWave> scheduledBlockWaves = new ArrayList<>();
        private final List<net.minecraft.world.entity.item.FallingBlockEntity> activeWaveBlocks = new ArrayList<>();

        GaleosAttackGoal(GaleosEntity mob, double speed) {
            this.mob = mob;
            this.speed = speed;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = mob.getTarget();
            return t != null && t.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity t = mob.getTarget();
            return t != null && t.isAlive();
        }

        @Override
        public void start() {
            this.target = mob.getTarget();
            this.attackTimer = 0;
            this.cooldown = 0;
            this.pathRecalcTimer = 0;
        }

        @Override
        public void stop() {
            mob.setAttackState(0);
            attackTimer = 0;
            mob.getNavigation().stop();
            scheduledBlockWaves.clear(); // ← 追加
            activeWaveBlocks.clear();    // ← 追加
        }

        @Override
        public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void tick() {
            LivingEntity t = mob.getTarget();
            tickScheduledBlockWaves();
            tickWaveBlockDamage();

            if (t == null || !t.isAlive()) {
                mob.setAttackState(0);
                return;
            }
            this.target = t;

            // 頭は常にターゲットを見る（体の向きには影響しない）
            mob.getLookControl().setLookAt(t, 30F, 30F);

            if (cooldown > 0) {
                cooldown--;
                tryRecalcPath(t);
                return;
            }

            if (mob.getAttackState() > 0) {
                attackTimer++;
                mob.getNavigation().stop();
                faceTarget(t); // ← 攻撃中のみ体を強制的に向ける
                executeAttack(t);
                return;
            }

            double distSq = mob.distanceToSqr(t);
            if (distSq <= ATTACK_START_SQ) {
                mob.getNavigation().stop();
                faceTarget(t); // ← 攻撃直前も体を向ける
                startAttack();
            } else {
                // ── 移動中は体の向きをナビゲーションに任せる ──
                tryRecalcPath(t);
            }
        }

        // 経路の再計算を間引く。毎tick moveTo() すると、ターゲットとの位置関係が
        // 軸に近い(=当たり判定ボックスの辺に正対する)ときに左右の迂回経路が
        // 同コストで揺れてしまい、体が小刻みに回転/斜め移動する原因になる。
        private void tryRecalcPath(LivingEntity t) {
            pathRecalcTimer--;
            boolean navDone = mob.getNavigation().isDone();
            boolean targetMoved = target != null
                    && target.distanceToSqr(t.getX(), t.getY(), t.getZ()) > 1.0D;

            if (pathRecalcTimer <= 0 || navDone || targetMoved) {
                pathRecalcTimer = 10 + mob.getRandom().nextInt(5); // 10〜14tickごと
                mob.getNavigation().moveTo(t, speed);
            }
        }

        // 体（yBodyRot）を滑らかにターゲットへ向ける
        private void faceTarget(LivingEntity t) {
            double dx = t.getX() - mob.getX();
            double dz = t.getZ() - mob.getZ();
            float targetYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);

            // 停止して向き直す場面でのみ呼ばれるので、少し速めに回転してOK
            float maxTurn = 25F;

            float newYaw = rotlerp(mob.getYRot(), targetYaw, maxTurn);

            mob.setYRot(newYaw);
            mob.yBodyRot = newYaw;
            mob.yHeadRot = newYaw;
        }

        private static float rotlerp(float current, float target, float maxChange) {
            float diff = net.minecraft.util.Mth.wrapDegrees(target - current);

            if (diff > maxChange) diff = maxChange;
            if (diff < -maxChange) diff = -maxChange;

            return current + diff;
        }

        private void startAttack() {
            int roll = mob.random.nextInt(3);
            mob.setAttackState(roll + 1); // 1,2,3
            attackTimer = 0;
        }

        private void executeAttack(LivingEntity t) {
            int atk = mob.getAttackState();

            if (attackTimer == ATK_HIT) {
                switch (atk) {
                    case 1 -> doArmSwipe(1.0);   // attack1：右腕
                    case 2 -> doArmSwipe(-1.0);  // attack1_2：左腕
                    case 3 -> doDoubleSlam();    // attack2：両腕叩きつけ
                }
            }

            if (attackTimer >= ATK_TOTAL) {
                finishAttack(30);
            }
        }

        private void doArmSwipe(double side) {
            Vec3 look = mob.getLookAngle();
            AABB box = mob.getBoundingBox()
                    .move(look.x * 2.5, 0, look.z * 2.5)
                    .inflate(3.0, 2.0, 3.0);

            for (LivingEntity e : getHittableEntities(box)) {
                e.hurt(mob.damageSources().mobAttack(mob),
                        (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE));

                Vec3 kb = e.position().subtract(mob.position()).normalize().scale(1.8);
                e.setDeltaMovement(kb.x, 0.5, kb.z);
                e.hurtMarked = true;
            }
            mob.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.8F);
        }

        private void doDoubleSlam() {
            AABB box = mob.getBoundingBox().inflate(4.0, 2.0, 4.0);

            for (LivingEntity e : getHittableEntities(box)) {
                e.hurt(mob.damageSources().mobAttack(mob),
                        (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.3F);

                Vec3 kb = e.position().subtract(mob.position()).normalize().scale(2.2);
                e.setDeltaMovement(kb.x, 0.7, kb.z);
                e.hurtMarked = true;
            }
            mob.playSound(SoundEvents.GENERIC_EXPLODE, 1.5F, 0.6F);
            mob.sendScreenShake(1.5F, 15);

            // ── 扇状ブロック波 ──
            if (mob.level() instanceof ServerLevel) {
                final BlockPos center = mob.blockPosition();
                double angle = Math.atan2(mob.getLookAngle().z, mob.getLookAngle().x);

                for (int r = 1; r <= 6; r++) {
                    scheduledBlockWaves.add(new ScheduledBlockWave(r * 2, center, r,
                            angle - Math.toRadians(60),
                            angle + Math.toRadians(60)));
                }
            }
        }

        private List<LivingEntity> getHittableEntities(AABB box) {
            return mob.level().getEntitiesOfClass(LivingEntity.class, box,
                    e -> e != mob && e.isAlive() && !(e instanceof GaleosEntity));
        }

        private void finishAttack(int cd) {
            attackTimer = 0;
            cooldown = cd;
            mob.setAttackState(0);
        }
        private void tickScheduledBlockWaves() {
            Iterator<ScheduledBlockWave> it = scheduledBlockWaves.iterator();
            while (it.hasNext()) {
                ScheduledBlockWave wave = it.next();
                wave.remainingTicks--;
                if (wave.remainingTicks <= 0) {
                    if (mob.level() instanceof ServerLevel sl) {
                        spawnBlockWaveRing(sl, wave.center, wave.radius,
                                wave.startAngle, wave.endAngle);
                    }
                    it.remove();
                }
            }
        }

        private void spawnBlockWaveRing(ServerLevel level, BlockPos center,
                                        int currentRadius, double startAngle, double endAngle) {
            for (int x = -currentRadius; x <= currentRadius; x++) {
                for (int z = -currentRadius; z <= currentRadius; z++) {
                    double distance = Math.sqrt(x * x + z * z);
                    if (distance >= currentRadius - 0.5 && distance <= currentRadius + 0.5) {

                        if (startAngle > -Math.PI * 2) {
                            double blockAngle = Math.atan2(z, x);
                            double arcSize = angleDiff(endAngle, startAngle);
                            double diffFromStart = angleDiff(blockAngle, startAngle);
                            if (diffFromStart > arcSize) continue;
                        }

                        BlockPos targetPos = center.offset(x, -1, z);
                        net.minecraft.world.level.block.state.BlockState state = level.getBlockState(targetPos);

                        if (!state.isAir() && state.getDestroySpeed(level, targetPos) >= 0) {
                            FallingBlockEntity fallingBlock =
                                    new net.minecraft.world.entity.item.FallingBlockEntity(
                                            net.minecraft.world.entity.EntityType.FALLING_BLOCK, level);
                            fallingBlock.setPos(
                                    targetPos.getX() + 0.5D,
                                    targetPos.getY() + 1.0D,
                                    targetPos.getZ() + 0.5D
                            );

                            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
                            fallingBlock.saveWithoutId(tag);
                            tag.put("BlockState", net.minecraft.nbt.NbtUtils.writeBlockState(state));
                            tag.putInt("Time", 580);
                            tag.putBoolean("DropItem", false);
                            tag.putBoolean("NoPhysics", true);
                            fallingBlock.load(tag);

                            fallingBlock.noPhysics = true;
                            fallingBlock.setDeltaMovement(0, 0.4D, 0);

                            level.addFreshEntity(fallingBlock);
                            activeWaveBlocks.add(fallingBlock);
                        }
                    }
                }
            }
        }

        private static double angleDiff(double a, double b) {
            double diff = a - b;
            while (diff < 0) diff += Math.PI * 2;
            while (diff > Math.PI * 2) diff -= Math.PI * 2;
            return diff;
        }

        private static class ScheduledBlockWave {
            int remainingTicks;
            BlockPos center;
            int radius;
            double startAngle;
            double endAngle;

            ScheduledBlockWave(int remainingTicks, BlockPos center, int radius,
                               double startAngle, double endAngle) {
                this.remainingTicks = remainingTicks;
                this.center = center;
                this.radius = radius;
                this.startAngle = startAngle;
                this.endAngle = endAngle;
            }
        }

        private void tickWaveBlockDamage() {
            if (mob.level().isClientSide()) return;

            activeWaveBlocks.removeIf(block -> {
                if (!block.isAlive()) return true;

                AABB hitBox = block.getBoundingBox().inflate(0.1);
                List<LivingEntity> hits = mob.level().getEntitiesOfClass(
                        LivingEntity.class, hitBox,
                        e -> e != mob && e.isAlive() && e.invulnerableTime <= 0
                );

                for (LivingEntity e : hits) {
                    e.hurt(
                            mob.damageSources().mobAttack(mob),
                            (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.8F
                    );
                    Vec3 kb = e.position().subtract(mob.position())
                            .normalize().scale(1.5);
                    e.setDeltaMovement(kb.x, 0.5, kb.z);
                    e.hurtMarked = true;
                }

                return false;
            });
        }
    }

    // ──────────────────────────────────────────
    // アニメーション
    // ──────────────────────────────────────────
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "base_controller", 5, state -> {
            if (this.getAttackState() > 0) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
            }
            if (state.isMoving()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));

        // ── コンストラクタの第3引数にtransitionTickTimeを直接指定 ──
        controllers.add(new AnimationController<>(this, "action_controller", 3, state -> {
            AnimationController<?> controller = state.getController();
            boolean hasAnim = controller.getCurrentAnimation() != null;
            boolean isFinished = controller.hasAnimationFinished();

            int atk = this.getAttackState();

            // ---- 攻撃中 ----
            if (atk > 0) {
                if (this.previousAttackState != atk) {
                    controller.forceAnimationReset();
                    this.previousAttackState = atk;
                }
                String animName = switch (atk) {
                    case 1 -> "attack1";
                    case 2 -> "attack1_2";
                    case 3 -> "attack2";
                    default -> "idle";
                };
                return state.setAndContinue(RawAnimation.begin().thenPlay(animName));
            }

            // ---- 攻撃終了後（attackState == 0） ----
            if (hasAnim && !isFinished) {
                // まだアニメーションが終わっていない → 継続
                return PlayState.CONTINUE;
            } else {
                // アニメーションが終了した、または何も再生されていない → 停止して base に移行
                this.previousAttackState = 0;
                return PlayState.STOP;
            }
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }
}