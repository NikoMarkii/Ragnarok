package com.niko.ragnarok.entity.Projectile;

import com.niko.ragnarok.entity.RagnarokEntities;
import com.niko.ragnarok.config.RagnarokConfig;
import com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.DinocampusEntity;
import net.minecraft.core.particles.DustParticleOptions;
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
import org.joml.Vector3f;
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
    private Vec3 spiralCenter;
    private double spiralRadius;
    private double spiralAngle;
    private static final double SPIRAL_RADIAL_SPEED = 0.18D;
    private static final double SPIRAL_TANGENTIAL_SPEED = 0.32D;
    private static final Map<UUID, Integer> SUFFOCATION_TICKS = new HashMap<>();

    // 赤い泡（追尾する火の泡）は、当たらなくてもこのtick数が経過したら自然に弾ける
    private static final int RED_LIFETIME_TICKS = 60;
    private static final DustParticleOptions BLUE_TRAIL =
            new DustParticleOptions(new Vector3f(0.12F, 0.7F, 1.0F), 1.2F);

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

    public void startSpiral(Vec3 center, double angle) {
        this.spiralCenter = center;
        this.spiralRadius = 0.5D;
        this.spiralAngle = angle;
        this.setPos(center.x + Math.cos(angle) * this.spiralRadius, center.y,
                center.z + Math.sin(angle) * this.spiralRadius);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.setVariant(tag.getInt("Variant"));
        this.spiralCenter = tag.contains("SpiralCenterX")
                ? new Vec3(tag.getDouble("SpiralCenterX"), tag.getDouble("SpiralCenterY"), tag.getDouble("SpiralCenterZ"))
                : null;
        this.spiralRadius = tag.getDouble("SpiralRadius");
        this.spiralAngle = tag.getDouble("SpiralAngle");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Variant", this.getVariant());
        if (this.spiralCenter != null) {
            tag.putDouble("SpiralCenterX", this.spiralCenter.x);
            tag.putDouble("SpiralCenterY", this.spiralCenter.y);
            tag.putDouble("SpiralCenterZ", this.spiralCenter.z);
            tag.putDouble("SpiralRadius", this.spiralRadius);
            tag.putDouble("SpiralAngle", this.spiralAngle);
        }
    }

    @Override
    public void tick() {
        this.clearFire();
        super.tick();
        this.clearFire();

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
        if (this.level().isClientSide() && !this.isBursting()) {
            spawnTrail();
        }

        // 赤い泡は追尾し続けて当たらないまま長引くことがあるため、専用の寿命で先に弾けさせる
        if (this.getVariant() == RED && this.tickCount > RED_LIFETIME_TICKS) {
            burst(null);
            return;
        }

        // ★変更: 単なる消滅ではなく、時間経過（160tick）ですべての泡が割れる（burst）ようにする
        if (this.tickCount > 160) {
            burst(null);
        }
    }

    // グラディウス装備（防御力24・タフネス12）向けにダメージを上方修正
    private void hit(LivingEntity living) {
        LivingEntity sourceOwner = this.owner;
        // magic()は防御力を貫通するダメージタイプなので使わない。
        // ownerがいればmobAttack(防御力を通常通り計算)、いなければgeneric(同じく通常通り)にする。
        DamageSource source = sourceOwner != null
                ? this.damageSources().mobAttack(sourceOwner)
                : this.damageSources().generic();

        // 直前の被弾による無敵時間で、この泡のダメージだけ判定漏れするのを防ぐ
        living.invulnerableTime = 0;

        switch (this.getVariant()) {
            case RED -> {
                living.hurt(source, RagnarokConfig.scaleSpecialDamage(sourceOwner, 14.0F));
                living.setSecondsOnFire(5);
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
            }
            case YELLOW -> {
                living.hurt(source, RagnarokConfig.scaleSpecialDamage(sourceOwner, 16.0F));
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
                living.hurt(source, RagnarokConfig.scaleSpecialDamage(sourceOwner, 12.0F));
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
            if (this.spiralCenter != null) {
                this.spiralRadius += SPIRAL_RADIAL_SPEED;
                // Cap tangential speed so outer bubbles do not accelerate or skip targets.
                this.spiralAngle += Math.min(0.12D, SPIRAL_TANGENTIAL_SPEED / this.spiralRadius);
                double nextX = this.spiralCenter.x + Math.cos(this.spiralAngle) * this.spiralRadius;
                double nextZ = this.spiralCenter.z + Math.sin(this.spiralAngle) * this.spiralRadius;
                this.setDeltaMovement(nextX - this.getX(), 0.0D, nextZ - this.getZ());
            } else {
                this.setDeltaMovement(current.x, 0.0D, current.z);
            }
            this.setPos(this.getX(), this.level().getHeightmapPos(
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    this.blockPosition()).getY() + 0.08D, this.getZ());
        }

    }

    // Continuous trails are client-side to avoid a packet per projectile per tick.
    private void spawnTrail() {
        Vec3 direction = this.getDeltaMovement().normalize();
        Vec3 side = direction.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 0.001D) side = new Vec3(1, 0, 0);
        side = side.normalize();
        Vec3 up = side.cross(direction).normalize();
        for (int i = 0; i < 2; i++) {
            double angle = this.tickCount * 0.55D + i * Math.PI;
            Vec3 offset = side.scale(Math.cos(angle) * 0.35D).add(up.scale(Math.sin(angle) * 0.35D));
            Vec3 point = this.position().add(0, this.getBbHeight() * 0.5D, 0).add(offset);
            var particle = switch (this.getVariant()) {
                case RED -> ParticleTypes.FLAME;
                case YELLOW -> ParticleTypes.ELECTRIC_SPARK;
                default -> BLUE_TRAIL;
            };
            this.level().addParticle(particle, point.x, point.y, point.z,
                    -direction.x * 0.03D, 0.02D, -direction.z * 0.03D);
        }
        if (this.tickCount % 3 == 0) {
            this.level().addParticle(this.getVariant() == RED ? ParticleTypes.SMOKE : ParticleTypes.SPLASH,
                    this.getX(), this.getY() + 0.1D, this.getZ(), 0, 0.02D, 0);
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
            // A short radial burst marks the actual hit, before the burst animation finishes.
            for (int i = 0; i < 16; i++) {
                double angle = Math.PI * 2.0D * i / 16;
                sl.sendParticles(this.getVariant() == BLUE ? ParticleTypes.SPLASH : particle,
                        this.getX() + Math.cos(angle) * 0.45D, this.getY() + 0.3D,
                        this.getZ() + Math.sin(angle) * 0.45D,
                        0, Math.cos(angle) * 0.2D, 0.08D, Math.sin(angle) * 0.2D, 1.0D);
            }
            if (this.getVariant() == RED) {
                sl.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(),
                        10, 0.4D, 0.4D, 0.4D, 0.04D);
                sl.playSound(null, this.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 0.8F, 1.2F);
            } else if (this.getVariant() == YELLOW) {
                sl.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY() + 0.3D, this.getZ(), 1, 0, 0, 0, 0);
            }
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
