package com.niko.ragnarok.entity.Projectile;

import com.niko.ragnarok.entity.RagnarokEntities;
import com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.DinocampusEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class DinocampusBubbleEntity extends Entity implements GeoEntity {
    public static final int BLUE = 0;
    public static final int RED = 1;
    public static final int YELLOW = 2;

    private static final EntityDataAccessor<Integer> VARIANT =
            SynchedEntityData.defineId(DinocampusBubbleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> BURSTING =
            SynchedEntityData.defineId(DinocampusBubbleEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private LivingEntity owner;
    private LivingEntity target;
    private int burstTicks;
    private static final Map<UUID, Integer> SUFFOCATION_TICKS = new HashMap<>();

    public DinocampusBubbleEntity(EntityType<? extends DinocampusBubbleEntity> type, Level level) {
        super(type, level);
    }

    public DinocampusBubbleEntity(Level level, LivingEntity owner, LivingEntity target, int variant, Vec3 position, Vec3 velocity) {
        this(RagnarokEntities.DINOCAMPUS_BUBBLE.get(), level);
        this.owner = owner;
        this.target = target;
        this.setVariant(variant);
        this.setPos(position.x, position.y, position.z);
        this.setDeltaMovement(velocity);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(VARIANT, BLUE);
        this.entityData.define(BURSTING, false);
    }

    public int getVariant() {
        return this.entityData.get(VARIANT);
    }

    public void setVariant(int variant) {
        this.entityData.set(VARIANT, variant);
    }

    public boolean isBursting() {
        return this.entityData.get(BURSTING);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.setVariant(tag.getInt("Variant"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Variant", this.getVariant());
    }

    @Override
    public void tick() {
        super.tick();

        if (this.isBursting()) {
            this.burstTicks++;

            // ★追加: 16.6tick（約17tick）目で泡と水のパーティクルを発生させる
            if (this.burstTicks == 17) {
                if (this.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.BUBBLE, this.getX(), this.getY(), this.getZ(), 15, 0.35D, 0.35D, 0.35D, 0.05D);
                    sl.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getY(), this.getZ(), 15, 0.35D, 0.35D, 0.35D, 0.05D);
                }
            }

            if (this.burstTicks > 30) {
                this.discard();
            }
            return;
        }

        if (!this.level().isClientSide()) {
            tickServerMovement();
            tickServerCollision();
        }

        Vec3 movement = this.getDeltaMovement();
        this.move(MoverType.SELF, movement);

        // ★変更: 単なる消滅ではなく、時間経過（160tick）ですべての泡が割れる（burst）ようにする
        if (this.tickCount > 160) {
            burst(null);
        }
    }

    // グラディウス装備（防御力24・タフネス12）向けにダメージを上方修正
    private void hit(LivingEntity living) {
        LivingEntity sourceOwner = this.owner;
        DamageSource source = sourceOwner != null
                ? this.damageSources().mobAttack(sourceOwner)
                : this.damageSources().magic();

        switch (this.getVariant()) {
            case RED -> {
                living.hurt(source, 14.0F); // ★ 8.0F -> 14.0F
                living.setSecondsOnFire(5);
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
            }
            case YELLOW -> {
                living.hurt(source, 16.0F); // ★ 9.0F -> 16.0F
                if (this.level() instanceof ServerLevel sl) {
                    net.minecraft.world.entity.LightningBolt lightning =
                            net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(sl);
                    if (lightning != null) {
                        lightning.moveTo(living.getX(), living.getY(), living.getZ());
                        lightning.setVisualOnly(false);
                        sl.addFreshEntity(lightning);
                    }
                }
            }
            default -> {
                living.hurt(source, 12.0F); // ★ 6.0F -> 12.0F
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                addSuffocation(living, 60);
            }
        }
        burst(living);
    }

    private void tickServerMovement() {
        int variant = this.getVariant();

        if (variant == RED && this.target != null && this.target.isAlive()) {
            Vec3 desired = this.target.position()
                    .add(0.0D, this.target.getBbHeight() * 0.5D, 0.0D)
                    .subtract(this.position())
                    .normalize()
                    .scale(0.42D);
            Vec3 current = this.getDeltaMovement();
            this.setDeltaMovement(current.scale(0.88D).add(desired.scale(0.12D)));
        } else if (variant == YELLOW) {
            Vec3 current = this.getDeltaMovement();
            this.setDeltaMovement(current.x, 0.0D, current.z);
            this.setPos(this.getX(), this.level().getHeightmapPos(
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    this.blockPosition()).getY() + 0.08D, this.getZ());
        }

        if (this.level() instanceof ServerLevel sl) {
            var particle = switch (variant) {
                case RED -> ParticleTypes.FLAME;
                case YELLOW -> ParticleTypes.ELECTRIC_SPARK;
                default -> ParticleTypes.BUBBLE;
            };
            sl.sendParticles(particle, this.getX(), this.getY(), this.getZ(), 2, 0.12D, 0.12D, 0.12D, 0.01D);
        }
    }

    private void tickServerCollision() {
        AABB box = this.getBoundingBox().inflate(0.25D);
        for (LivingEntity living : this.level().getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity.isAlive() && entity != this.owner && !(entity instanceof DinocampusEntity))) {
            hit(living);
            return;
        }

        if (this.horizontalCollision || (this.getVariant() != YELLOW && this.verticalCollision)) {
            burst(null);
        }
    }

    public static void addSuffocation(LivingEntity living, int ticks) {
        SUFFOCATION_TICKS.put(living.getUUID(), ticks);
    }

    public static void tickSuffocation(LivingEntity living) {
        Integer ticks = SUFFOCATION_TICKS.get(living.getUUID());
        if (ticks == null) {
            return;
        }

        if (!living.isAlive() || ticks <= 0) {
            SUFFOCATION_TICKS.remove(living.getUUID());
            return;
        }

        if (!living.level().isClientSide() && ticks % 20 == 0) {
            living.hurt(living.damageSources().drown(), 1.5F);
            if (living.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.BUBBLE_POP,
                        living.getX(), living.getY() + living.getBbHeight() * 0.5D, living.getZ(),
                        12, 0.35D, 0.35D, 0.35D, 0.04D);
            }
        }

        SUFFOCATION_TICKS.put(living.getUUID(), ticks - 1);
    }

    public static void cleanupSuffocationCache() {
        Iterator<Map.Entry<UUID, Integer>> iterator = SUFFOCATION_TICKS.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue() <= 0) {
                iterator.remove();
            }
        }
    }

    private void burst(LivingEntity hitEntity) {
        if (this.isBursting()) {
            return;
        }

        this.entityData.set(BURSTING, true);
        this.setDeltaMovement(Vec3.ZERO);
        this.burstTicks = 0;

        if (this.level() instanceof ServerLevel sl) {
            var particle = switch (this.getVariant()) {
                case RED -> ParticleTypes.FLAME;
                case YELLOW -> ParticleTypes.ELECTRIC_SPARK;
                default -> ParticleTypes.BUBBLE_POP;
            };
            sl.sendParticles(particle, this.getX(), this.getY(), this.getZ(), 24, 0.35D, 0.35D, 0.35D, 0.05D);
        }

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.HOSTILE, 1.0F, 0.9F);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main_controller", 2, state -> {
            if (this.isBursting()) {
                return state.setAndContinue(RawAnimation.begin().thenPlayAndHold("burst"));
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop("floating"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
