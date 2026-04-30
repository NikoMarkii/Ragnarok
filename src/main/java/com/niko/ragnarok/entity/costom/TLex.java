package com.niko.ragnarok.entity.costom;

import com.niko.ragnarok.entity.RagnarokEntities;
import com.niko.ragnarok.entity.ai.TlexAttackGoal;
import com.niko.ragnarok.sound.RagnarokSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

public class TLex extends Animal {

    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState attackAnimationState = new AnimationState();

    public final AnimationState walkAnimationState = new AnimationState();

    public final AnimationState roarAnimationState = new AnimationState();

    private static final EntityDataAccessor<Boolean> ATTACKING =
            SynchedEntityData.defineId(TLex.class, EntityDataSerializers.BOOLEAN);

    public boolean hasDealtBiteDamage = false;
    public int attackAnimationTimeout = 31;

    private static final EntityDataAccessor<Boolean> ROARING =
            SynchedEntityData.defineId(TLex.class, EntityDataSerializers.BOOLEAN);

    private int roarTicks = 0;

    private int roarCooldown = 0;public int attackAnimTickCounter = 0;
    private LivingEntity lastTarget;

    public TLex(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ATTACKING, false);
        this.entityData.define(ROARING, false);
    }
    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions size) {
        return 3.6F; // 実際の目の位置（モデルの頭部あたり）
    }

    public boolean isAttacking() {
        return this.entityData.get(ATTACKING);
    }

    public void setAttacking(boolean attacking) {
        this.entityData.set(ATTACKING, attacking);
    }

    public boolean isRoaring() {
        return this.entityData.get(ROARING);
    }

    public void setRoaring(boolean roaring) { this.entityData.set(ROARING, roaring); }
    private void breakLeaves() {
        // サーバー側でのみ実行し、10tickに1回程度の頻度にして負荷を抑える
        if (!this.level().isClientSide && this.tickCount % 10 == 0) {
            // TLexの周囲（当たり判定の範囲）の座標を取得
            AABB boundingBox = this.getBoundingBox().inflate(0.5D, 0.5D, 0.5D);

            int minX = Mth.floor(boundingBox.minX);
            int minY = Mth.floor(boundingBox.minY);
            int minZ = Mth.floor(boundingBox.minZ);
            int maxX = Mth.floor(boundingBox.maxX);
            int maxY = Mth.floor(boundingBox.maxY);
            int maxZ = Mth.floor(boundingBox.maxZ);

            for (int x = minX; x <= maxX; ++x) {
                for (int y = minY; y <= maxY; ++y) {
                    for (int z = minZ; z <= maxZ; ++z) {
                        BlockPos blockPos = new BlockPos(x, y, z);
                        BlockState blockState = this.level().getBlockState(blockPos);

                        // 破壊対象を「葉っぱ（Leaves）」タグを持つブロックに限定する
                        if (blockState.is(BlockTags.LEAVES)) {
                            // ブロックを破壊（trueでアイテムドロップあり、falseで消滅のみ）
                            this.level().destroyBlock(blockPos, true, this);

                            // 破壊した時の音を鳴らす（オプション）
                            this.level().playSound(null, blockPos, SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
                        }
                    }
                }
            }
        }
    }
    // TLex.java
    private void setupAnimationStates() {
        // 咆哮優先
        if (this.isRoaring()) {
            this.roarAnimationState.startIfStopped(this.tickCount);
            this.attackAnimationState.stop();
            this.idleAnimationState.stop();
            return; // 咆哮中はここで終了
        } else {
            this.roarAnimationState.stop();
        }

        // 攻撃アニメーション
        if (this.isAttacking()) {
            // startIfStopped を使うことで、再生中に何度も最初に戻るのを防ぐ
            this.attackAnimationState.startIfStopped(this.tickCount);
            this.idleAnimationState.stop();
        } else {
            this.attackAnimationState.stop();
            // 何もしていない時だけIdle
            this.idleAnimationState.startIfStopped(this.tickCount);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            setupAnimationStates();
        } else {
            // サーバー側のタイマー管理（ここが止まるとフラグが下りない）
            if (this.roarTicks > 0) {
                this.roarTicks--;
                if (this.roarTicks <= 0) this.setRoaring(false);
            }

            // 攻撃タイマー：これがないと isAttacking が true のまま固定されてしまう
            if (this.isAttacking()) {
                if (this.attackAnimationTimeout > 0) {
                    this.attackAnimationTimeout--;
                } else {
                    this.setAttacking(false);
                }
            }

            // 咆哮のトリガー（ターゲットが変わった瞬間）
            LivingEntity target = this.getTarget();
            if (target != null && target != lastTarget && roarCooldown <= 0) {
                this.triggerRoar();
            }
            if (roarCooldown > 0) roarCooldown--;
            this.lastTarget = target;

            // 葉っぱ破壊
            this.breakLeaves();
        }
    }

    private void triggerRoar() {
        this.roarTicks = 60; // 咆哮の長さを少し余裕持って設定
        this.roarCooldown = 400;
        this.setRoaring(true);

        this.level().playSound(null, this.blockPosition(),
                RagnarokSoundEvents.TLEX_ROAR.get(), this.getSoundSource(),
                2.0F, 0.8F + this.random.nextFloat() * 0.2F);
    }
    // 攻撃処理（噛みつき）
    public void performBiteAttack(LivingEntity target) {
        if (target != null && this.distanceTo(target) < 4.0D) {
            float damage = 12.0F;
            target.hurt(this.damageSources().mobAttack(this), damage);
            target.knockback(0.6F, this.getX() - target.getX(), this.getZ() - target.getZ());
            this.level().playSound(null, this.blockPosition(),
                    SoundEvents.RAVAGER_ATTACK, this.getSoundSource(), 1.2F, 0.8F);
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TlexAttackGoal(this, 0.5D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.3D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8f));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Pig.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Cow.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Villager.class, true));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Sheep.class, true));
        this.setMaxUpStep(1.5F); // 1.5ブロック分までの段差なら歩いて登れるようになる
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 150D)
                .add(Attributes.FOLLOW_RANGE, 24D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 2.0D);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return RagnarokEntities.T_LEX.get().create(level);
    }

    @Override
    public boolean isFood(ItemStack pStack) {
        return pStack.is(Items.COOKED_BEEF);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return RagnarokSoundEvents.TLEX_AMBIENT.get();
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundEvents.ENDER_DRAGON_HURT;
    }
    @Override
    protected void playStepSound(BlockPos pos, BlockState blockIn) {
        float volume = 0.4F + this.random.nextFloat() * 0.2F;
        float pitch = 0.8F + this.random.nextFloat() * 0.2F;

        this.level().playSound(
                null,
                this.getX(), this.getY(), this.getZ(),
                RagnarokSoundEvents.TLEX_STEP.get(),
                SoundSource.HOSTILE,   // ★ ここも同様に
                volume,
                pitch
        );
    }
    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return RagnarokSoundEvents.TLEX_AMBIENT.get();
    }
    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }
    @Override
    public void knockback(double strength, double xRatio, double zRatio) {
    }
}
