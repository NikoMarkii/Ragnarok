package com.niko.ragnarok.entity.costom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class Groot extends Monster {
    private int rushCooldown = 0;
    private int attackTick = 0;
    private boolean isAttacking = false;

    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState attack1AnimationState = new AnimationState();

    public final AnimationState walkAnimationState = new AnimationState();

    private static final byte START_ATTACK_1_EVENT = 4;

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
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.25D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == START_ATTACK_1_EVENT) {
            this.idleAnimationState.stop();
            this.attack1AnimationState.start(this.tickCount);
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (isAttacking) {
            attackTick++;

            // 1.25秒（25チック）の瞬間に踏み込みとダメージ判定
            // ※20チック = 1秒計算
            if (attackTick == 25) {
                // 前方にベクトル（推進力）を加える
                double d0 = Math.sin(this.getYRot() * (Math.PI / 180.0));
                double d1 = -Math.cos(this.getYRot() * (Math.PI / 180.0));
                // 0.5Dの部分を大きくすると、より遠くに踏み込むよ
                this.setDeltaMovement(this.getDeltaMovement().add(d0 * 0.5D, 0.0D, d1 * 0.5D));

                // ここで周囲の敵にダメージを与える（attack1のダメージ判定）
                this.doHurtTarget(this.getTarget());
            }

            // 2秒（40チック）で攻撃終了
            if (attackTick >= 40) {
                isAttacking = false;
                attackTick = 0;
            }
        }
    }

    // 攻撃を開始するメソッド（AIゴールから呼び出す）
    public void startAttack1() {
        if (!isAttacking) {
            isAttacking = true;
            attackTick = 0;
            // クライアント側に「攻撃アニメーション開始」のパケットを送る（AnimationState用）
            this.level().broadcastEntityEvent(this, (byte) 4);
        }
    }

    // 2ブロックの段差を越えるための設定
    @Override
    public float getStepHeight() {
        return 2.0F;
    }




    public void performSlamAttack() {
        if (!this.level().isClientSide) {
            BlockPos center = this.blockPosition().relative(this.getDirection());

            BlockPos.betweenClosedStream(center.offset(-1, 0, -1), center.offset(1, 2, 1)).forEach(pos -> {
                BlockState state = this.level().getBlockState(pos);

                if (state.getDestroySpeed(this.level(), pos) >= 0 && state.getDestroySpeed(this.level(), pos) <= 2.0F) {
                    this.level().destroyBlock(pos, true, this);
                }
            });

        }
    }
    public boolean isAngry() {
        return this.entityData.get(IS_ANGRY);
    }

    public void setAngry(boolean angry) {
        this.entityData.set(IS_ANGRY, angry);
    }
    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {

            if (this.getDeltaMovement().horizontalDistanceSqr() > 0.0001D) {
                if (!this.walkAnimationState.isStarted()) {
                    this.walkAnimationState.start(this.tickCount);
                }
            } else {
                this.walkAnimationState.stop();
            }
        }
    }
}