package com.niko.ragnarok.entity.geckolib_entity.Costom;

import com.niko.ragnarok.entity.Mid_Boss_Monster;
import com.niko.ragnarok.entity.others.EnderSoldierBeam;
import com.niko.ragnarok.entity.others.RkBodyRotationControl;
import com.niko.ragnarok.entity.others.RkCombatUtil;
import com.niko.ragnarok.entity.others.RkSmoothMoveControl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import org.joml.Vector3f;
import net.minecraftforge.event.ForgeEventFactory;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.UUID;

public class EnderSoldierEntity extends Mid_Boss_Monster implements GeoEntity {
    public static final float BODY_WIDTH = 2.5F;
    public static final float BODY_HEIGHT = 6.0F;
    private static final int DEATH_DURATION = 50;
    private static final double WALK_DISTANCE_PER_TICK = 0.1D;
    private static final EntityDataAccessor<Integer> ATTACK_STATE =
            SynchedEntityData.defineId(EnderSoldierEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ENRAGED =
            SynchedEntityData.defineId(EnderSoldierEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> BEAM_TICK =
            SynchedEntityData.defineId(EnderSoldierEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> BEAM_DIRECTION =
            SynchedEntityData.defineId(EnderSoldierEntity.class, EntityDataSerializers.VECTOR3);
    private static final AttributeModifier ENRAGED_ATTACK_BOOST = new AttributeModifier(
            UUID.fromString("07832f4a-4537-4ee6-9945-fbce83e9bdd1"), "Ender soldier enraged attack",
            0.5D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("death");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int previousAttackState;
    private int targetChangeTime;
    private int beamCooldown;

    public EnderSoldierEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 20;
        this.moveControl = new RkSmoothMoveControl(this, 8.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 200.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 15.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ATTACK_STATE, 0);
        this.entityData.define(ENRAGED, false);
        this.entityData.define(BEAM_TICK, -1);
        this.entityData.define(BEAM_DIRECTION, new Vector3f(0, 0, 1));
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SoldierAttackGoal(this));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new RkBodyRotationControl(this);
    }

    public int getAttackState() {
        return this.entityData.get(ATTACK_STATE);
    }

    private void setAttackState(int state) {
        this.entityData.set(ATTACK_STATE, state);
        if (state != Attack.BEAM.id) {
            this.entityData.set(BEAM_TICK, -1);
        }
    }

    public int getBeamTick() {
        return this.entityData.get(BEAM_TICK);
    }

    public boolean isFiringBeam() {
        return isAlive() && getAttackState() == Attack.BEAM.id
                && getBeamTick() >= EnderSoldierBeam.START_TICK && getBeamTick() < EnderSoldierBeam.END_TICK;
    }

    public Vec3 getBeamDirection() {
        Vector3f direction = this.entityData.get(BEAM_DIRECTION);
        return new Vec3(direction.x, direction.y, direction.z).normalize();
    }

    public Vec3 getBeamOrigin() {
        return EnderSoldierBeam.mouthPosition(position(), yBodyRot, getBeamTick());
    }

    public Vec3 getBeamEnd(Vec3 origin) {
        Vec3 end = origin.add(getBeamDirection().scale(EnderSoldierBeam.RANGE));
        return this.level().clip(new ClipContext(origin, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this))
                .getLocation();
    }

    public boolean isEnraged() {
        return this.entityData.get(ENRAGED);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        AABB bounds = super.getBoundingBoxForCulling();
        if (isEnraged()) {
            bounds = bounds.inflate(10.0D);
        }
        if (isFiringBeam()) {
            Vec3 origin = getBeamOrigin();
            bounds = bounds.minmax(new AABB(origin, origin.add(getBeamDirection().scale(EnderSoldierBeam.RANGE))).inflate(2.0D));
        }
        return bounds;
    }

    private void updateEnragedState() {
        if (this.level().isClientSide || !this.isAlive()) {
            return;
        }
        if (!isEnraged() && this.getHealth() <= this.getMaxHealth() * 0.5F) {
            this.entityData.set(ENRAGED, true);
            this.playSound(SoundEvents.ENDERMAN_SCREAM, 1.0F, 0.7F);
        }
        AttributeInstance attack = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (isEnraged() && attack != null && !attack.hasModifier(ENRAGED_ATTACK_BOOST)) {
            attack.addTransientModifier(ENRAGED_ATTACK_BOOST);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source) || !this.isAlive()) {
            return false;
        }
        if (source.is(DamageTypeTags.IS_PROJECTILE) || source.getDirectEntity() instanceof ThrownPotion) {
            boolean damaged = false;
            if (source.getDirectEntity() instanceof ThrownPotion potion
                    && PotionUtils.getPotion(potion.getItem()) == Potions.WATER
                    && PotionUtils.getMobEffects(potion.getItem()).isEmpty()) {
                damaged = super.hurt(source, amount);
                if (damaged) {
                    updateEnragedState();
                }
            }
            for (int attempt = 0; attempt < 64; attempt++) {
                if (teleport()) {
                    return true;
                }
            }
            return damaged;
        }
        boolean damaged = super.hurt(source, amount);
        if (damaged) {
            updateEnragedState();
        }
        if (!this.level().isClientSide && !(source.getEntity() instanceof LivingEntity)
                && this.random.nextInt(10) != 0) {
            teleport();
        }
        return damaged;
    }

    @Override
    public boolean isSensitiveToWater() {
        return true;
    }

    @Override
    public void setTarget(LivingEntity target) {
        if (target != this.getTarget()) {
            this.targetChangeTime = this.tickCount;
        }
        super.setTarget(target);
    }

    @Override
    protected void customServerAiStep() {
        if (this.level().isDay() && this.tickCount >= this.targetChangeTime + 600) {
            float brightness = this.getLightLevelDependentMagicValue();
            if (brightness > 0.5F && this.level().canSeeSky(this.blockPosition())
                    && this.random.nextFloat() * 30.0F < (brightness - 0.4F) * 2.0F) {
                this.setTarget(null);
                teleport();
            }
        }
        super.customServerAiStep();
    }

    private boolean teleport() {
        if (this.level().isClientSide || !this.isAlive()) {
            return false;
        }
        return tryTeleport(this.getX() + (this.random.nextDouble() - 0.5D) * 64.0D,
                this.getY() + this.random.nextInt(64) - 32,
                this.getZ() + (this.random.nextDouble() - 0.5D) * 64.0D);
    }

    private boolean teleportTowards(LivingEntity target) {
        Vec3 direction = new Vec3(this.getX() - target.getX(),
                this.getY(0.5D) - target.getEyeY(), this.getZ() - target.getZ()).normalize();
        return tryTeleport(this.getX() + (this.random.nextDouble() - 0.5D) * 8.0D - direction.x * 16.0D,
                this.getY() + this.random.nextInt(16) - 8 - direction.y * 16.0D,
                this.getZ() + (this.random.nextDouble() - 0.5D) * 8.0D - direction.z * 16.0D);
    }

    private boolean tryTeleport(double targetX, double targetY, double targetZ) {
        if (this.level().isClientSide || !this.isAlive()) {
            return false;
        }
        BlockPos.MutableBlockPos ground = new BlockPos.MutableBlockPos(targetX, targetY, targetZ);
        if (!this.level().hasChunkAt(ground)) {
            return false;
        }
        while (ground.getY() > this.level().getMinBuildHeight()
                && !this.level().getBlockState(ground).blocksMotion()) {
            ground.move(Direction.DOWN);
        }
        BlockState groundState = this.level().getBlockState(ground);
        if (!groundState.blocksMotion() || groundState.getFluidState().is(FluidTags.WATER)) {
            return false;
        }
        var event = ForgeEventFactory.onEnderTeleport(this, targetX, targetY, targetZ);
        if (event.isCanceled()) {
            return false;
        }
        Vec3 origin = this.position();
        if (!this.randomTeleport(event.getTargetX(), event.getTargetY(), event.getTargetZ(), true)) {
            return false;
        }
        this.setAttackState(0);
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        this.level().gameEvent(GameEvent.TELEPORT, origin, GameEvent.Context.of(this));
        if (!this.isSilent()) {
            this.level().playSound(null, origin.x, origin.y, origin.z,
                    SoundEvents.ENDERMAN_TELEPORT, this.getSoundSource(), 1.0F, 1.0F);
            this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
        }
        return true;
    }

    @Override
    public void aiStep() {
        updateEnragedState();
        if (!this.level().isClientSide && beamCooldown > 0) {
            beamCooldown--;
        }
        if (this.level().isClientSide && this.isAlive()) {
            for (int particleIndex = 0; particleIndex < 2; particleIndex++) {
                this.level().addParticle(ParticleTypes.PORTAL,
                        this.getRandomX(0.5D), this.getRandomY() - 0.25D, this.getRandomZ(0.5D),
                        (this.random.nextDouble() - 0.5D) * 2.0D, -this.random.nextDouble(),
                        (this.random.nextDouble() - 0.5D) * 2.0D);
            }
        }
        if (this.isDeadOrDying()) {
            this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
        }
        super.aiStep();
    }

    @Override
    public void die(DamageSource source) {
        this.setAttackState(0);
        this.getNavigation().stop();
        super.die(source);
    }

    @Override
    protected void tickDeath() {
        this.deathTime++;
        if (this.deathTime >= DEATH_DURATION && !this.level().isClientSide && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Enraged", isEnraged());
        tag.putInt("BeamCooldown", beamCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(ENRAGED, tag.getBoolean("Enraged"));
        this.setAttackState(0);
        this.beamCooldown = Mth.clamp(tag.getInt("BeamCooldown"), 0, EnderSoldierBeam.COOLDOWN);
        updateEnragedState();
    }
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENDERMAN_AMBIENT; 
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDERMAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDERMAN_DEATH;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            state.getController().setAnimationSpeed(1.0D);
            if (this.isDeadOrDying()) {
                state.getController().transitionLength(0);
                return state.setAndContinue(DEATH);
            }
            int attackId = getAttackState();
            if (attackId != previousAttackState) {
                state.getController().forceAnimationReset();
                previousAttackState = attackId;
            }
            if (attackId > 0 && attackId <= Attack.values().length) {
                return state.setAndContinue(Attack.values()[attackId - 1].animation);
            }
            if (state.isMoving()) {
                double speed = this.getDeltaMovement().horizontalDistance();
                state.getController().setAnimationSpeed(Mth.clamp(speed / WALK_DISTANCE_PER_TICK, 0.25D, 2.5D));
                return state.setAndContinue(WALK);
            }
            return state.setAndContinue(IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private enum Attack {
        LEFT_SWEEP(1, 25, 45, 4.5D, 140.0D, 1.0F, 0.45D),
        RIGHT_SWEEP(2, 25, 45, 4.5D, 140.0D, 1.0F, 0.45D),
        SLAM(3, 20, 40, 5.0D, 80.0D, 1.25F, 0.7D),
        BEAM(4, EnderSoldierBeam.START_TICK, EnderSoldierBeam.DURATION,
                EnderSoldierBeam.RANGE, 0.0D, 0.4F, 0.0D);

        private final int id;
        private final int impactTick;
        private final int duration;
        private final double reach;
        private final double arc;
        private final float damageMultiplier;
        private final double knockback;
        private final RawAnimation animation;

        Attack(int id, int impactTick, int duration, double reach, double arc,
               float damageMultiplier, double knockback) {
            this.id = id;
            this.impactTick = impactTick;
            this.duration = duration;
            this.reach = reach;
            this.arc = arc;
            this.damageMultiplier = damageMultiplier;
            this.knockback = knockback;
            this.animation = RawAnimation.begin().thenPlay("attack" + id);
        }
    }

    private static class SoldierAttackGoal extends Goal {
        private static final double ATTACK_START_RANGE = 4.0D;
        private static final double CHASE_SPEED_MULTIPLIER = 1.25D;
        private static final double ATTACK_STEP_SPEED = 1.1D;
        private static final int ATTACK_COOLDOWN = 12;
        private static final Attack[] MELEE_ATTACKS = {Attack.LEFT_SWEEP, Attack.RIGHT_SWEEP, Attack.SLAM};
        private final EnderSoldierEntity mob;
        private Attack attack;
        private LivingEntity lockedTarget;
        private int attackTick;
        private int cooldown;
        private int previousAttackId;
        private int teleportTime;
        private float attackYaw;

        SoldierAttackGoal(EnderSoldierEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private boolean isValidTarget(LivingEntity target) {
            return target != null && target.isAlive() && !target.isSpectator()
                    && (!(target instanceof Player player) || !player.isCreative());
        }

        @Override
        public boolean canUse() {
            return mob.isAlive() && isValidTarget(mob.getTarget());
        }

        @Override
        public boolean canContinueToUse() {
            return mob.isAlive() && (attack != null || isValidTarget(mob.getTarget()));
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void stop() {
            mob.getNavigation().stop();
            mob.setAttackState(0);
            attack = null;
            lockedTarget = null;
            attackTick = 0;
            cooldown = ATTACK_COOLDOWN;
            teleportTime = 0;
        }

        @Override
        public void tick() {
            if (attack != null && mob.getAttackState() == 0) {
                stop();
                return;
            }
            if (attack != null) {
                tickAttack();
                return;
            }
            LivingEntity target = mob.getTarget();
            if (!isValidTarget(target)) {
                return;
            }
            if (cooldown > 0) {
                cooldown--;
            }
            if (cooldown == 0 && canUseBeam(target)) {
                startAttack(Attack.BEAM, target);
                mob.beamCooldown = EnderSoldierBeam.COOLDOWN;
                mob.entityData.set(BEAM_TICK, 0);
                mob.playSound(SoundEvents.ENDERMAN_SCREAM, 0.8F, 0.65F);
                return;
            }
            if (!mob.isPassenger() && mob.distanceToSqr(target) > 256.0D) {
                if (++teleportTime >= 30 && mob.teleportTowards(target)) {
                    teleportTime = 0;
                    cooldown = ATTACK_COOLDOWN;
                    return;
                }
            } else {
                teleportTime = 0;
            }
            mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (mob.distanceToSqr(target) > ATTACK_START_RANGE * ATTACK_START_RANGE
                    || !mob.hasLineOfSight(target)) {
                mob.getNavigation().moveTo(target, CHASE_SPEED_MULTIPLIER);
                return;
            }
            mob.getNavigation().stop();
            RkCombatUtil.faceTarget(mob, target);
            if (cooldown == 0) {
                int nextAttackId;
                do {
                    nextAttackId = 1 + mob.getRandom().nextInt(MELEE_ATTACKS.length);
                } while (nextAttackId == previousAttackId);
                startAttack(MELEE_ATTACKS[nextAttackId - 1], target);
            }
        }

        private boolean canUseBeam(LivingEntity target) {
            if (!mob.isEnraged() || mob.beamCooldown > 0 || mob.isPassenger()
                    || mob.distanceToSqr(target) > EnderSoldierBeam.RANGE * EnderSoldierBeam.RANGE
                    || !mob.hasLineOfSight(target)) {
                return false;
            }
            AABB bounds = target.getBoundingBox();
            double distanceX = Mth.clamp(mob.getX(), bounds.minX, bounds.maxX) - mob.getX();
            double distanceZ = Mth.clamp(mob.getZ(), bounds.minZ, bounds.maxZ) - mob.getZ();
            return distanceX * distanceX + distanceZ * distanceZ > Attack.SLAM.reach * Attack.SLAM.reach
                    || bounds.minY > mob.getBoundingBox().maxY || bounds.maxY < mob.getY() - 0.5D;
        }

        private void startAttack(Attack nextAttack, LivingEntity target) {
            mob.getNavigation().stop();
            RkCombatUtil.faceTarget(mob, target);
            attack = nextAttack;
            previousAttackId = attack.id;
            lockedTarget = target;
            attackTick = 0;
            attackYaw = mob.getYRot();
            mob.setAttackState(attack.id);
        }

        private void tickAttack() {
            attackTick++;
            mob.getNavigation().stop();
            mob.setDeltaMovement(0.0D, mob.getDeltaMovement().y, 0.0D);
            if (attack == Attack.BEAM) {
                tickBeam();
                return;
            }
            if (attackTick <= attack.impactTick - 5 && isValidTarget(lockedTarget)) {
                RkCombatUtil.faceTarget(mob, lockedTarget, 15.0F);
                attackYaw = mob.getYRot();
            }
            mob.setYRot(attackYaw);
            mob.yBodyRot = attackYaw;
            mob.yHeadRot = attackYaw;
            if (attackTick == attack.impactTick) {
                performAttack();
            }
            if (attackTick >= attack.duration) {
                mob.setAttackState(0);
                attack = null;
                lockedTarget = null;
                cooldown = ATTACK_COOLDOWN;
            }
        }

        private void tickBeam() {
            if (attackTick <= EnderSoldierBeam.START_TICK) {
                if (!isValidTarget(lockedTarget)) {
                    stop();
                    return;
                }
                RkCombatUtil.faceTarget(mob, lockedTarget, 8.0F);
                attackYaw = mob.getYRot();
            }
            mob.setYRot(attackYaw);
            mob.yBodyRot = attackYaw;
            mob.yHeadRot = attackYaw;
            mob.entityData.set(BEAM_TICK, attackTick);
            if (attackTick == EnderSoldierBeam.START_TICK) {
                Vec3 aim = lockedTarget.getBoundingBox().getCenter().subtract(mob.getBeamOrigin()).normalize();
                mob.entityData.set(BEAM_DIRECTION, aim.toVector3f());
                mob.playSound(SoundEvents.GUARDIAN_ATTACK, 1.2F, 0.6F);
            }
            if (mob.isFiringBeam() && (attackTick - EnderSoldierBeam.START_TICK) % EnderSoldierBeam.DAMAGE_INTERVAL == 0) {
                damageAlongBeam();
            }
            if (attackTick >= EnderSoldierBeam.DURATION) {
                stop();
            }
        }

        private void damageAlongBeam() {
            Vec3 origin = mob.getBeamOrigin();
            Vec3 end = mob.getBeamEnd(origin);
            if (origin.distanceToSqr(end) < 1.0E-6D) {
                return;
            }
            AABB area = new AABB(origin, end).inflate(EnderSoldierBeam.RADIUS);
            float damage = (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE) * Attack.BEAM.damageMultiplier;
            for (LivingEntity target : mob.level().getEntitiesOfClass(LivingEntity.class, area)) {
                if (!isValidTarget(target) || target == mob || target instanceof EnderSoldierEntity || mob.isAlliedTo(target)) {
                    continue;
                }
                AABB bounds = target.getBoundingBox();
                AABB expanded = bounds.inflate(EnderSoldierBeam.RADIUS);
                var intersection = expanded.clip(origin, end);
                if (!expanded.contains(origin) && intersection.isEmpty()) {
                    continue;
                }
                Vec3 contact = intersection.orElse(origin);
                Vec3 surface = new Vec3(Mth.clamp(contact.x, bounds.minX, bounds.maxX),
                        Mth.clamp(contact.y, bounds.minY, bounds.maxY), Mth.clamp(contact.z, bounds.minZ, bounds.maxZ));
                var obstacle = mob.level().clip(new ClipContext(origin, surface,
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob));
                if (obstacle.getType() != HitResult.Type.MISS
                        && obstacle.getLocation().distanceToSqr(origin) + 1.0E-6D < surface.distanceToSqr(origin)) {
                    continue;
                }
                target.hurt(mob.damageSources().mobAttack(mob), damage);
                if (!mob.isAlive()) {
                    break;
                }
            }
        }

        private void performAttack() {
            double yaw = Math.toRadians(attackYaw);
            Vec3 forward = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
            mob.setDeltaMovement(forward.x * ATTACK_STEP_SPEED, mob.getDeltaMovement().y,
                    forward.z * ATTACK_STEP_SPEED);
            double minimumDot = Math.cos(Math.toRadians(attack.arc * 0.5D));
            AABB area = new AABB(mob.getX() - attack.reach, mob.getY() - 0.5D, mob.getZ() - attack.reach,
                    mob.getX() + attack.reach, mob.getY() + mob.getBbHeight(), mob.getZ() + attack.reach);
            float damage = (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE) * attack.damageMultiplier;
            for (LivingEntity target : mob.level().getEntitiesOfClass(LivingEntity.class, area)) {
                if (!isValidTarget(target) || target == mob || target instanceof EnderSoldierEntity
                        || mob.isAlliedTo(target) || !mob.hasLineOfSight(target)) {
                    continue;
                }
                AABB bounds = target.getBoundingBox();
                double closestX = Mth.clamp(mob.getX(), bounds.minX, bounds.maxX);
                double closestZ = Mth.clamp(mob.getZ(), bounds.minZ, bounds.maxZ);
                double distanceSquared = (closestX - mob.getX()) * (closestX - mob.getX())
                        + (closestZ - mob.getZ()) * (closestZ - mob.getZ());
                Vec3 offset = new Vec3(target.getX() - mob.getX(), 0.0D, target.getZ() - mob.getZ());
                if (distanceSquared > attack.reach * attack.reach
                        || (offset.lengthSqr() > 1.0E-6D && forward.dot(offset.normalize()) < minimumDot)) {
                    continue;
                }
                if (target.hurt(mob.damageSources().mobAttack(mob), damage)) {
                    target.knockback(attack.knockback, -forward.x, -forward.z);
                }
            }
            mob.playSound(attack == Attack.SLAM ? SoundEvents.TRIDENT_RIPTIDE_3 : SoundEvents.TRIDENT_THROW,
                    1.0F, attack == Attack.SLAM ? 0.7F : 0.85F);
        }
    }
}
