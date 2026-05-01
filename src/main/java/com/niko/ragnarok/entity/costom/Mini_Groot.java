package com.niko.ragnarok.entity.costom;

import com.niko.ragnarok.entity.ai.MiniGrootMeleeAttackGoal;
import com.niko.ragnarok.entity.ai.MiniGrootSpinAttackGoal;
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
                this.level().broadcastEntityEvent(this, (byte)7); // 簡易的にハートを出す
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
}
