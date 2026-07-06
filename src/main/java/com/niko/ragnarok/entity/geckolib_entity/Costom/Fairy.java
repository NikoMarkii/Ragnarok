package com.niko.ragnarok.entity.geckolib_entity.Costom;

import com.niko.ragnarok.item.Ragnarok_mainItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

/**
 * フェアリー - 森に住む友好的な妖精
 *
 * 特徴:
 * - 森バイオームにスポーン
 * - 常に飛行し、地面には降りない
 * - 攻撃されると逃げる（戦闘能力なし）
 * - 花を通貨として取引可能
 * - 複数のバリエーションが存在
 */
public class Fairy extends PathfinderMob implements GeoEntity, FlyingAnimal, Merchant {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Nullable
    private Player tradingPlayer; // 現在取引中のプレイヤー
    @Nullable
    protected MerchantOffers offers; // 取引内容のリスト


    // データシンク - バリエーション（0-5の6種類）
    private static final EntityDataAccessor<Integer> VARIANT =
            SynchedEntityData.defineId(Fairy.class, EntityDataSerializers.INT);

    // 逃走中フラグ
    private static final EntityDataAccessor<Boolean> IS_FLEEING =
            SynchedEntityData.defineId(Fairy.class, EntityDataSerializers.BOOLEAN);

    public Fairy(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        // 20は方向転換の速さ。trueは空中移動を有効にする設定だ。
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
    }

    @Override
    protected void registerGoals() {
        // 優先度0: パニック（攻撃されたら逃げる）
        this.goalSelector.addGoal(0, new FairyPanicGoal(this, 1.4D));

        // 優先度2: ランダムに飛び回る
        this.goalSelector.addGoal(2, new FairyWanderGoal(this));

        // 優先度3: プレイヤーを見る
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 6.0F));

        // 優先度4: ランダムに見回す
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.goalSelector.addGoal(1, new TemptGoal(this, 1.25D, Ingredient.of(ItemTags.FLOWERS), false));

        this.goalSelector.addGoal(0, new Goal() {
            @Override
            public boolean canUse() {
                return Fairy.this.getTradingPlayer() != null;
            }

            @Override
            public void start() {
                Fairy.this.getNavigation().stop();
            }

            @Override
            public void tick() {
                // 取引中は速度をゼロに固定して、空中静止させる
                Fairy.this.setDeltaMovement(Vec3.ZERO);
                // さらに、プレイヤーの方をしっかり向かせる
                Fairy.this.getLookControl().setLookAt(Fairy.this.getTradingPlayer(), 30.0F, 30.0F);
            }
        });
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0D)
                .add(Attributes.FLYING_SPEED, 0.4D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.FOLLOW_RANGE, 35.0D);
    }
    @Override
    public void aiStep() {
        super.aiStep();

        if (this.level().isClientSide && this.random.nextFloat() < 0.1F) {
            // キラキラパーティクル
            this.level().addParticle(
                    ParticleTypes.END_ROD,
                    this.getX() + (this.random.nextDouble() - 0.5D) * 0.5D,
                    this.getY() + this.random.nextDouble() * 1.2D,
                    this.getZ() + (this.random.nextDouble() - 0.5D) * 0.5D,
                    0, 0.02D, 0
            );
        }
    }
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && !this.isFleeing()) {
            if (this.offers == null) {
                this.updateTrades(); // 取引内容がなければ作成
            }
            this.setTradingPlayer(player);
            this.openTradingScreen(player, this.getDisplayName(), 1);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    // 取引内容を生成するメソッド
    protected void updateTrades() {
        this.offers = new MerchantOffers();
        this.offers.add(new MerchantOffer(
                new ItemStack(Items.DANDELION, 12),
                new ItemStack(Items.APPLE),
                16, 2, 0.05F));
        this.offers.add(new MerchantOffer(
                new ItemStack(Items.OXEYE_DAISY, 8), // ヒナギク 8個
                new ItemStack(Ragnarok_mainItems.GLOWING_DUST.get(), 1), // 鱗粉 1個
                5,  // 最大取引回数
                10, // 獲得経験値
                0.05F // 価格倍率
        ));
        this.offers.add(new MerchantOffer(
                new ItemStack(Items.POPPY, 10),
                new ItemStack(Items.DANDELION, 10),
                new ItemStack(Ragnarok_mainItems.FLOWER_NECKLACE.get()),
                3, 15, 0.05F));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(false);
        navigation.setCanPassDoors(true);
        return navigation;
    }
    public static boolean isBrightEnoughToSpawn(BlockAndTintGetter level, BlockPos pos) {
        // 明るさレベルが8より大きい（つまり9以上）ならスポーンを許可する
        return level.getRawBrightness(pos, 0) > 8;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VARIANT, 0);
        this.entityData.define(IS_FLEEING, false);
    }

    public int getVariant() {
        return this.entityData.get(VARIANT);
    }

    public void setVariant(int variant) {
        this.entityData.set(VARIANT, variant);
    }

    public boolean isFleeing() {
        return this.entityData.get(IS_FLEEING);
    }

    public void setFleeing(boolean fleeing) {
        this.entityData.set(IS_FLEEING, fleeing);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnData,
                                        @Nullable CompoundTag tag) {
        // ランダムなバリエーションを設定（0-5の6種類）
        this.setVariant(this.random.nextInt(2));
        return super.finalizeSpawn(level, difficulty, spawnType, spawnData, tag);
    }
    @Override
    public void setTradingPlayer(@Nullable Player player) {
        this.tradingPlayer = player;
    }

    @Override
    public @Nullable Player getTradingPlayer() {
        return this.tradingPlayer;
    }

    @Override
    public MerchantOffers getOffers() {
        if (this.offers == null) {
            this.updateTrades();
        }
        return this.offers;
    }

    @Override
    public void overrideOffers(MerchantOffers offers) {
        this.offers = offers;
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        this.ambientSoundTime = -this.getAmbientSoundInterval();
        // 取引成立時にキラキラを出す
        if (this.level() instanceof ServerLevel) {
            ((ServerLevel)this.level()).sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 1.0D, this.getZ(), 10, 0.5, 0.5, 0.5, 0.02);
        }
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {}

    @Override
    public int getVillagerXp() { return 0; }

    @Override
    public void overrideXp(int xp) {}

    @Override
    public boolean showProgressBar() { return false; }

    @Override
    public SoundEvent getNotifyTradeSound() { return SoundEvents.ALLAY_ITEM_TAKEN; }

    @Override
    public boolean isClientSide() { return this.level().isClientSide; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", this.getVariant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setVariant(tag.getInt("Variant"));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        }

        // ダメージを受けたら逃走モードに
        this.setFleeing(true);

        return super.hurt(source, amount);
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        // 落下ダメージなし
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ALLAY_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ALLAY_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    @Override
    public boolean isFlying() {
        return !this.onGround();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main_controller", 5, state -> {
            // 1. 移動速度を確認（微小な動きを許容するために 0.01 程度を閾値にする）
            double velocity = this.getDeltaMovement().horizontalDistanceSqr();
            boolean isMoving = velocity > 0.001; // ほぼ動いていなければ false

            // 2. 地面にいない（飛行中）かつ、動いているなら fly
            if (!this.onGround() && isMoving) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("fly"));
            }

            // 3. それ以外（地面にいる、もしくは空中で静止している）なら idle
            return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    /**
     * フェアリー専用のパニックゴール
     * 攻撃されたら上空へ逃げる
     */
    static class FairyPanicGoal extends Goal {
        private final Fairy fairy;
        private final double speedModifier;
        private double posX;
        private double posY;
        private double posZ;

        public FairyPanicGoal(Fairy fairy, double speedModifier) {
            this.fairy = fairy;
            this.speedModifier = speedModifier;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            // 最後に受けたダメージから5秒以内なら逃走
            return this.fairy.getLastHurtByMobTimestamp() > this.fairy.tickCount - 100;
        }

        @Override
        public void start() {
            // ランダムな方向の上空へ逃げる
            Vec3 currentPos = this.fairy.position();
            this.posX = currentPos.x + (this.fairy.getRandom().nextDouble() - 0.5D) * 16.0D;
            this.posY = currentPos.y + 8.0D + this.fairy.getRandom().nextDouble() * 4.0D;
            this.posZ = currentPos.z + (this.fairy.getRandom().nextDouble() - 0.5D) * 16.0D;

            this.fairy.setFleeing(true);
        }

        @Override
        public boolean canContinueToUse() {
            return !this.fairy.getNavigation().isDone() &&
                    this.fairy.getLastHurtByMobTimestamp() > this.fairy.tickCount - 100;
        }

        @Override
        public void tick() {
            this.fairy.getNavigation().moveTo(this.posX, this.posY, this.posZ, this.speedModifier);
        }

        @Override
        public void stop() {
            this.fairy.setFleeing(false);
        }
    }

    /**
     * フェアリー専用の徘徊ゴール
     * 空中をふわふわ飛び回る
     */
    static class FairyWanderGoal extends Goal {
        private final Fairy fairy;

        public FairyWanderGoal(Fairy fairy) {
            this.fairy = fairy;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            // nextInt(10) を nextInt(100) くらいにすると、
            // 目的地に到着した後、しばらくその場でホバリングするようになるよ。
            return this.fairy.getNavigation().isDone() &&
                    this.fairy.getRandom().nextInt(100) == 0 &&
                    !this.fairy.isFleeing();
        }

        @Override
        public boolean canContinueToUse() {
            return this.fairy.getNavigation().isInProgress();
        }

        @Override
        public void start() {
            Vec3 target = this.getRandomPosition();
            if (target != null) {
                this.fairy.getNavigation().moveTo(target.x, target.y, target.z, 1.0D);
            }
        }

        @Nullable
        private Vec3 getRandomPosition() {
            Vec3 current = this.fairy.position();

            // 現在位置から8ブロック以内のランダムな位置
            double x = current.x + (this.fairy.getRandom().nextDouble() - 0.5D) * 16.0D;
            double y = current.y + (this.fairy.getRandom().nextDouble() - 0.5D) * 4.0D;
            double z = current.z + (this.fairy.getRandom().nextDouble() - 0.5D) * 16.0D;

            // 地面から最低3ブロック以上の高さを保つ
            BlockPos groundPos = new BlockPos((int)x, (int)y, (int)z);
            while (groundPos.getY() > this.fairy.level().getMinBuildHeight() &&
                    !this.fairy.level().getBlockState(groundPos).isSolidRender(this.fairy.level(), groundPos)) {
                groundPos = groundPos.below();
            }

            y = Math.max(y, groundPos.getY() + 3.0D);

            return new Vec3(x, y, z);
        }
    }
}
