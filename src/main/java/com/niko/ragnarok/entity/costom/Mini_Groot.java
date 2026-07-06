package com.niko.ragnarok.entity.costom;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class Mini_Groot extends TamableAnimal {
    // アニメーション状態の定義
    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState walkAnimationState = new AnimationState();
    public final AnimationState sitAnimationState = new AnimationState();

    public final AnimationState attackAnimationState = new AnimationState();

    // 回転攻撃の3段階
    public final AnimationState spinStartAnimationState = new AnimationState();
    public final AnimationState spinLoopAnimationState = new AnimationState();
    public final AnimationState spinEndAnimationState = new AnimationState();

    public int spinAttackTimer = 0;

    private static final EntityDataAccessor<Boolean> IS_ATTACKING =
            SynchedEntityData.defineId(Mini_Groot.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_SPINNING =
            SynchedEntityData.defineId(Mini_Groot.class, EntityDataSerializers.BOOLEAN);

    public Mini_Groot(EntityType<? extends TamableAnimal> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new MiniGrootMeleeAttackGoal(this, 1.6D, true)); // 攻撃優先
        this.goalSelector.addGoal(4, new MiniGrootSpinAttackGoal(this));
        this.goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.3D, 10.0F, 2.0F, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1D));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            setupAnimationStates();
        } else {
            // サーバー側でタイマーを更新
            if (this.spinAttackTimer > 0) {
                this.spinAttackTimer--;
                if (this.spinAttackTimer <= 0) {
                    this.entityData.set(IS_SPINNING, false);
                }
            }
        }
    }
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        // 1. 手なずけ処理（未手なずけ時）
        if (itemstack.is(Items.GOLDEN_APPLE) || itemstack.is(Items.GOLDEN_CARROT)) {
            if (!this.isTame()) {
                if (!player.getAbilities().instabuild) {
                    itemstack.shrink(1);
                }
                if (this.random.nextInt(3) == 0) { // 33%で成功
                    this.tame(player);
                    this.navigation.stop();
                    this.setTarget(null);
                    this.setOrderedToSit(true);
                    this.level().broadcastEntityEvent(this, (byte)7); // ハート
                } else {
                    this.level().broadcastEntityEvent(this, (byte)6); // 煙
                }
                return InteractionResult.SUCCESS;
            }
        }

        // 2. 骨粉による回復処理（手なずけ済み & 自分の持ち物）
        if (this.isTame() && this.isOwnedBy(player) && itemstack.is(Items.BONE_MEAL)) {
            if (this.getHealth() < this.getMaxHealth()) {
                if (!player.getAbilities().instabuild) {
                    itemstack.shrink(1);
                }
                this.heal(4.0F); // ハート2個分回復

                // 回復時のパーティクル（緑のキラキラ）と音
                this.level().levelEvent(1505, this.blockPosition(), 0);
                this.playSound(SoundEvents.BONE_MEAL_USE, 1.0F, 1.0F);

                return InteractionResult.SUCCESS;
            } else {
                // 体力が満タンなら、お座り処理に行かせないために「CONSUME」か「SUCCESS」を返す
                return InteractionResult.PASS;
            }
        }

        // 3. お座り切り替え（手なずけ済み & 自分の持ち物 & 骨粉を持っていない時）
        if (this.isTame() && this.isOwnedBy(player)) {
            this.setOrderedToSit(!this.isOrderedToSit());
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }
    private void setupAnimationStates() {
        if (this.isAttacking()) {
            this.attackAnimationState.startIfStopped(this.tickCount);
            this.walkAnimationState.stop();
            this.idleAnimationState.stop();
            return;
        }
        this.attackAnimationState.stop();

        if (this.isInSittingPose()) {
            this.sitAnimationState.startIfStopped(this.tickCount);
            this.walkAnimationState.stop();
            this.idleAnimationState.stop();
        } else {
            this.sitAnimationState.stop();
            if (this.isMoving()) {
                this.walkAnimationState.startIfStopped(this.tickCount);
                this.idleAnimationState.stop();
            } else {
                this.idleAnimationState.startIfStopped(this.tickCount);
                this.walkAnimationState.stop();
            }
        }
        if (this.isSpinning()) {
            this.spinLoopAnimationState.startIfStopped(this.tickCount);
        } else {
            this.spinLoopAnimationState.stop();
        }
    }
    public void setAttacking(boolean attacking) {
        this.entityData.set(IS_ATTACKING, attacking);
    }

    public boolean isAttacking() {
        return this.entityData.get(IS_ATTACKING);
    }
    public void setSpinning(boolean spinning) {
        this.entityData.set(IS_SPINNING, spinning);
    }

    public boolean isSpinning() {
        return this.entityData.get(IS_SPINNING);
    }
    private boolean isMoving() {
        return this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6D;
    }
    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel pLevel, AgeableMob pOtherParent) {
        return null;
    }
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IS_SPINNING, false);
        this.entityData.define(IS_ATTACKING, false);
    }
    class MiniGrootMeleeAttackGoal extends MeleeAttackGoal {
        private final Mini_Groot mob;
        private int attackTimer = 0; // アニメーション完遂用タイマー

        public MiniGrootMeleeAttackGoal(Mini_Groot mob, double speedModifier, boolean followingTargetEvenIfNotSeen) {
            super(mob, speedModifier, followingTargetEvenIfNotSeen);
            this.mob = mob;
        }

        @Override
        public void tick() {
            // 1. 移動やターゲット確認（super.tick() を呼ぶと内部で攻撃判定が走り、
            // クールタイムが勝手に加算されることがあるので注意が必要だ）
            super.tick();

            LivingEntity target = this.mob.getTarget();
            if (target == null) return;

            // 2. アニメーション進行中の処理
            if (this.attackTimer > 0) {
                this.attackTimer--;

                // ダメージ発生（10tick目）
                if (this.attackTimer == 4) {
                    this.mob.doHurtTarget(target);
                }

                // 【ここが重要】アニメーションが終わった瞬間
                if (this.attackTimer <= 0) {
                    this.mob.setAttacking(false);

                    // バニラの MeleeAttackGoal クラスにある「次に殴れるまでの時間」を
                    // 強制的に 0 に書き換えて、即座に次の canUse / 攻撃判定を許可する
                    this.resetAttackCooldown(); // これで ticksUntilNextAttack = 0 になる
                }
            } else {
                // 3. 攻撃待機中の処理（自前で距離をチェック）
                double distanceSq = this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ());

                // 射程内かつ、バニラ側のクールタイムが 0 以下なら次へ
                if (distanceSq <= 12.25D && this.getTicksUntilNextAttack() <= 0) {
                    this.mob.setAttacking(true);
                    this.attackTimer = 14;
                    // 攻撃開始時に一度リセット（これを行わないと super.tick 内で二重にカウントされることがある）
                    this.resetAttackCooldown();
                }
            }
        }
        @Override
        protected void resetAttackCooldown() {
            super.resetAttackCooldown();
        }
        @Override
        protected double getAttackReachSqr(LivingEntity pAttackTarget) {
            // 基本の射程に余裕（+1.0程度）を持たせることで、密着状態のミスを防ぐ
            return (double)(this.mob.getBbWidth() * 2.0F * this.mob.getBbWidth() * 2.0F + pAttackTarget.getBbWidth()) + 1.0D;
        }
        private void startAttackAnimation(LivingEntity pEnemy) {
            this.mob.setAttacking(true);
            this.attackTimer = 14;
            this.resetAttackCooldown();
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity pEnemy, double pDistToEnemySqr) {
        }

        @Override
        public void stop() {
            super.stop();
            this.attackTimer = 0;
            this.mob.setAttacking(false);
        }
    }
    class MiniGrootSpinAttackGoal extends Goal {
        private final Mini_Groot mob;
        private LivingEntity target;
        private int attackTimer = 0;
        private int attackStep = 0;
        private int globalCooldown = 0; // 次に回転できるまでの時間

        public MiniGrootSpinAttackGoal(Mini_Groot mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            // クールタイムを減らす
            if (this.globalCooldown > 0) {
                this.globalCooldown--;
                return false;
            }

            this.target = this.mob.getTarget();
            if (this.target == null || !this.target.isAlive()) return false;

            double distanceSq = this.mob.distanceToSqr(this.target);

            // 【重要】通常攻撃の射程(9.0D)より外側、かつ16.0D以内の時だけ発動
            // これで「近くなら殴る、少し離れたら回る」という使い分けができる
            return distanceSq > 9.0D && distanceSq < 25.0D;
        }

        @Override
        public void stop() {
            this.attackStep = 0;
            this.mob.setSpinning(false);
            this.mob.getNavigation().stop();
            // 一度使ったら200チック（10秒間）は回転禁止だ！
            this.globalCooldown = 200;
        }

        @Override
        public void tick() {
            if (target == null) return;

            if (attackStep == 1) { // 開始
                this.mob.getLookControl().setLookAt(target);
                this.mob.getNavigation().moveTo(target, 1.2D);
                attackTimer++;
                if (attackTimer >= 17) {
                    attackStep = 2;
                    attackTimer = 0;
                }
            }
            else if (attackStep == 2) { // 回転中
                // --- ここが「物理的回転」の心臓部だ ---
                // 1チックごとに 72度回転させれば、5チック（0.25秒）で1回転する計算だ
                float nextYRot = this.mob.getYRot() + 72.0F;
                this.mob.setYRot(nextYRot);
                this.mob.setYBodyRot(nextYRot); // 体の向きも同期させる

                // ターゲットへにじり寄る
                this.mob.getNavigation().moveTo(target, 1.0D);
                attackTimer++;

                // ダメージ判定
                if (attackTimer % 5 == 0) {
                    this.mob.level().getEntitiesOfClass(LivingEntity.class, this.mob.getBoundingBox().inflate(1.5D)).forEach(e -> {
                        if (e != mob && !mob.isAlliedTo(e)) {
                            e.hurt(this.mob.damageSources().mobAttack(this.mob), 4.0F);
                        }
                    });
                }

                if (attackTimer >= 40) { // 2秒で終了
                    attackStep = 3;
                    attackTimer = 0;
                }
            }
            else if (attackStep == 3) { // 終了
                attackTimer++;
                if (attackTimer >= 15) this.stop();
            }
        }
    }
}
