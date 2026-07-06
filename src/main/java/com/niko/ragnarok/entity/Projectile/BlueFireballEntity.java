package com.niko.ragnarok.entity.Projectile;

import com.niko.ragnarok.entity.RagnarokEntities;
import com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.Gradius;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class BlueFireballEntity extends Projectile {

    private LivingEntity target;
    private static final float SPEED = 0.4F;
    private static final float TURN_RATE = 0.12F;

    public BlueFireballEntity(
            EntityType<? extends BlueFireballEntity> type,
            Level level) {
        super(type, level);
    }

    public BlueFireballEntity(
            Level level,
            LivingEntity owner,
            LivingEntity target) {
        super(RagnarokEntities.BLUE_FIREBALL.get(), level);
        this.target = target;
        setOwner(owner);

        // オーナーの少し前方にスポーン
        Vec3 ownerPos = owner.getEyePosition();
        setPos(ownerPos.x, ownerPos.y, ownerPos.z);

        // 初期速度をターゲット方向に設定
        if (target != null) {
            Vec3 dir = target.position()
                    .add(0, target.getBbHeight() * 0.5, 0)
                    .subtract(ownerPos)
                    .normalize()
                    .scale(SPEED);
            setDeltaMovement(dir);
        }
    }

    public void setTarget(LivingEntity target) {
        this.target = target;
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public void tick() {
        super.tick();

        // ホーミング処理
        if (target != null && target.isAlive()) {
            Vec3 toTarget = target.position()
                    .add(0, target.getBbHeight() * 0.5, 0)
                    .subtract(position())
                    .normalize()
                    .scale(SPEED);

            Vec3 current = getDeltaMovement();
            setDeltaMovement(
                    current.scale(1.0 - TURN_RATE)
                            .add(toTarget.scale(TURN_RATE))
            );
        }

        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(
                this,
                entity -> entity instanceof LivingEntity
                        && !(entity instanceof Gradius)  // グラディウスはスルー
                        && entity != getOwner()
                        && entity.isAlive()
        );
        if (hitResult.getType() != HitResult.Type.MISS) {
            onHit(hitResult);
        }

        // 位置更新
        Vec3 movement = getDeltaMovement();
        setPos(getX() + movement.x, getY() + movement.y, getZ() + movement.z);

        // ソウルファイアパーティクル（サーバー側のみ）
        if (!level().isClientSide() && level() instanceof ServerLevel sl) {
            sl.sendParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    getX(), getY(), getZ(),
                    2,       // 個数
                    0.15, 0.15, 0.15,  // ランダム広がり
                    0.01     // 速度
            );
        }

        // ★ tickCount > 100 の寿命削除 → プレイヤーに当たるまで消えない
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);

        Entity hit = result.getEntity();
        if (hit instanceof LivingEntity living
                && hit != getOwner()
                && !(hit instanceof Gradius)  // グラディウス自身には当たらない
                && !level().isClientSide()) {

            LivingEntity owner = (getOwner() instanceof LivingEntity l) ? l : null;
            DamageSource source = owner != null
                    ? damageSources().mobAttack(owner)
                    : damageSources().magic();

            living.hurt(source, 16.0F);
            discard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        // ★ ブロックに当たっても消えない（プレイヤーまで貫通）
        // discard() を呼ばない
    }
}
