package com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.Volcanic_Serpent;

import com.niko.ragnarok.entity.RagnarokEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public class VolcanicSerpentEntity extends Monster implements GeoEntity {

    // 胴体12個 + 尻尾1個
    public static final int BODY_SEGMENT_COUNT = 12;
    public static final int TOTAL_PART_COUNT = BODY_SEGMENT_COUNT + 1;

    // 既存モデルの寸法を基準にした初期値
    public static final double HEAD_REAR_JOINT_OFFSET = 23.0D / 16.0D;
    // 頭部から最初の胴体の中心まで
    public static final double HEAD_TO_FIRST_BODY =
            65.0D / 16.0D;
    // 胴体1個分の長さ
    public static final double BODY_SEGMENT_LENGTH =
            84.0D / 16.0D;
    // 最後の胴体中心から尻尾中心まで
    public static final double LAST_BODY_TO_TAIL =
            118.0D / 16.0D;

    private static final EntityDataAccessor<Optional<UUID>> FIRST_PART_UUID =
            SynchedEntityData.defineId(
                    VolcanicSerpentEntity.class,
                    EntityDataSerializers.OPTIONAL_UUID
            );

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    public VolcanicSerpentEntity(
            EntityType<? extends VolcanicSerpentEntity> type,
            Level level
    ) {
        super(type, level);

        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.LAVA, 0.0F);

        this.setNoGravity(true);
        this.xpReward = 250;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 600.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 18.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(FIRST_PART_UUID, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));

        this.goalSelector.addGoal(
                2,
                new RandomStrollGoal(this, 0.8D, 80)
        );

        this.goalSelector.addGoal(
                3,
                new LookAtPlayerGoal(this, Player.class, 12.0F)
        );

        this.goalSelector.addGoal(
                4,
                new RandomLookAroundGoal(this)
        );

        this.targetSelector.addGoal(
                1,
                new HurtByTargetGoal(this)
        );

        this.targetSelector.addGoal(
                2,
                new NearestAttackableTargetGoal<>(
                        this,
                        Player.class,
                        true
                )
        );
    }

    @Override
    public void tick() {
        super.tick();

        // パーツ生成はサーバー側だけで行う
        if (!this.level().isClientSide) {
            this.ensurePartsExist();
        }
    }

    /**
     * 初回だけ胴体・尻尾を生成する。
     */
    private void ensurePartsExist() {
        if (this.getFirstPart() != null) {
            return;
        }

        this.setFirstPartUUID(null);

        LivingEntity parent = this;

        for (int i = 0; i < TOTAL_PART_COUNT; i++) {

            VolcanicSerpentPartEntity part =
                    new VolcanicSerpentPartEntity(
                            RagnarokEntities.VOLCANIC_SERPENT_PART.get(),
                            this.level()
                    );

            part.setParent(parent);
            part.setBodyIndex(i);
            part.setTail(i == BODY_SEGMENT_COUNT);

            // 最初の胴体は頭部の後端から、
            // それ以降は直前のパーツから一定距離を取る
            double followDistance;

            if (i == 0) {
                // 頭部から最初の胴体
                followDistance = HEAD_TO_FIRST_BODY;
            } else if (i == BODY_SEGMENT_COUNT) {
                // 最後の胴体から尻尾
                followDistance = LAST_BODY_TO_TAIL;
            } else {
                // 胴体同士
                followDistance = BODY_SEGMENT_LENGTH;
            }

            part.setFollowDistance((float) followDistance);
            part.setInitialPartPos(parent);

            if (!this.level().addFreshEntity(part)) {
                break;
            }

            if (i == 0) {
                this.setFirstPartUUID(part.getUUID());
            }

            // 次のパーツは今回生成したパーツを親にする
            parent = part;
        }
    }

    @Nullable
    public UUID getFirstPartUUID() {
        return this.entityData.get(FIRST_PART_UUID).orElse(null);
    }

    public void setFirstPartUUID(@Nullable UUID uuid) {
        this.entityData.set(
                FIRST_PART_UUID,
                Optional.ofNullable(uuid)
        );
    }

    @Nullable
    public VolcanicSerpentPartEntity getFirstPart() {
        UUID uuid = this.getFirstPartUUID();

        if (uuid == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        if (serverLevel.getEntity(uuid) instanceof VolcanicSerpentPartEntity part) {
            return part;
        }

        return null;
    }

    @Override
    public void travel(Vec3 travelVector) {
        boolean inLiquid = this.isInLava() || this.isInWater();

        if (this.isEffectiveAi() && inLiquid) {
            // Bone Serpentを参考にした液体内移動
            this.moveRelative(this.getSpeed(), travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());

            this.setDeltaMovement(
                    this.getDeltaMovement().scale(0.9D)
            );

            if (this.getTarget() == null) {
                this.setDeltaMovement(
                        this.getDeltaMovement().add(0.0D, -0.005D, 0.0D)
                );
            }
        } else {
            super.travel(travelVector);
        }
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        if (level.getFluidState(pos).is(FluidTags.LAVA)
                || level.getFluidState(pos).is(FluidTags.WATER)) {
            return 10.0F;
        }

        return this.isInLava() ? Float.NEGATIVE_INFINITY : 0.0F;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEAD;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, amount);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // アニメーションはRenderer接続時に追加する
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
