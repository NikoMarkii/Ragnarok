package com.niko.ragnarok.entity.Projectile;

import com.niko.ragnarok.effect.ModMobEffects;
import com.niko.ragnarok.entity.RagnarokEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class VoidSlashEntity extends ThrowableItemProjectile {
    // データアクセサは変更なし
    private static final EntityDataAccessor<Boolean> IS_BIG = SynchedEntityData.defineId(VoidSlashEntity.class, EntityDataSerializers.BOOLEAN);

    public VoidSlashEntity(EntityType<? extends VoidSlashEntity> type, Level level) {
        super(type, level);
    }

    public VoidSlashEntity(Level level, LivingEntity owner) {
        super(RagnarokEntities.VOID_SLASH.get(), owner, level);
        // 生成時は巨大フラグはここでは設定しない（Scythe側で setBig を呼ぶため）
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        // 初期値は false（Scythe側で true に設定される）
        this.entityData.define(IS_BIG, false);
    }

    public void setBig(boolean big) {
        this.entityData.set(IS_BIG, big);
    }

    public boolean isBig() {
        return this.entityData.get(IS_BIG);
    }

    // tick内のパーティクル演出は変更なし
    @Override
    public void tick() {
        // super.tick() を呼ぶと重力が計算されてしまうため、
        // ファイアチャージのように「落ちない」動きを自前で制御する

        // 基本的な位置更新
        Vec3 vec3 = this.getDeltaMovement();
        double d0 = this.getX() + vec3.x;
        double d1 = this.getY() + vec3.y;
        double d2 = this.getZ() + vec3.z;
        this.updateRotation();

        // 1. 重力を無視し、空気抵抗も受けない（速度を維持）
        // 通常の投射物はここで 0.99 などを掛けて減速するが、そのまま維持させる
        this.setDeltaMovement(vec3);

        // 2. 水への衝突判定など、最低限必要な処理
        if (this.isInWater()) {
            for(int i = 0; i < 4; ++i) {
                this.level().addParticle(ParticleTypes.BUBBLE, d0 - vec3.x * 0.25D, d1 - vec3.y * 0.25D, d2 - vec3.z * 0.25D, vec3.x, vec3.y, vec3.z);
            }
        }

        // 3. 座標の更新と衝突判定の実行
        this.setPos(d0, d1, d2);
        this.checkInsideBlocks();

        // ThrowableItemProjectile の標準的な衝突判定を呼び出す
        HitResult hitresult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitresult.getType() != HitResult.Type.MISS) {
            this.onHit(hitresult);
        }

        // 4. パーティクル演出
        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.SOUL,
                    this.getX(), this.getY() + 0.1, this.getZ(), 0, 0, 0);
        }

        // 40tick（2秒）で消滅する設定はそのまま
        if (this.tickCount > 40) {
            this.discard();
        }
    }

    // 重力を無効化するオーバーライド（念のため）
    @Override
    protected float getGravity() {
        return 0.0F;
    }
    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide && result.getEntity() instanceof LivingEntity target) {
            Entity owner = this.getOwner();
            LivingEntity attacker = owner instanceof LivingEntity ? (LivingEntity) owner : null;

            // 巨大斬撃としての威力（12.0F）
            target.hurt(this.damageSources().indirectMagic(this, attacker), 12.0F);

            // エフェクト「死神の刻印」を付与
            target.addEffect(new MobEffectInstance(
                    ModMobEffects.VOID_REAPER_EFFECT.get(), 20, 2, false, true, true));

            // 演出の実行
            if (this.level() instanceof ServerLevel serverLevel) {
                spawnHitParticles(serverLevel, target);
            }
        }
    }

    /**
     * 移植：死神の刻印の演出（音とパーティクル）
     * 以前 VoidScythe クラス内にあった spawnReaperParticles と activateReaperMark の演出を統合
     */
    private void spawnReaperMarkEffects(ServerLevel level, LivingEntity target) {
        // 音のエフェクト
        level.playSound(null,
                target.getX(), target.getY(), target.getZ(),
                SoundEvents.ENDER_DRAGON_FLAP,
                SoundSource.PLAYERS,
                1.0F,
                1.8F
        );

        level.playSound(null,
                target.getX(), target.getY(), target.getZ(),
                SoundEvents.WITHER_HURT,
                SoundSource.PLAYERS,
                0.7F,
                0.5F
        );

        // パーティクルエフェクト
        Vec3 targetPos = target.position();

        // 螺旋状のパーティクル
        for (int i = 0; i < 30; i++) {
            double angle = Math.toRadians(i * 12);
            double height = i * 0.1D;
            double radius = 1.5D - (i * 0.04D);

            double x = targetPos.x + Math.cos(angle) * radius;
            double y = targetPos.y + height;
            double z = targetPos.z + Math.sin(angle) * radius;

            level.sendParticles(
                    ParticleTypes.WITCH,
                    x, y, z,
                    1,
                    0, 0, 0,
                    0.0D
            );

            level.sendParticles(
                    ParticleTypes.SOUL,
                    x, y, z,
                    1,
                    0, 0, 0,
                    0.0D
            );
        }

        // 中心の爆発エフェクト
        level.sendParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                targetPos.x,
                targetPos.y + 1.0D,
                targetPos.z,
                15,
                0.5D, 0.5D, 0.5D,
                0.05D
        );
    }
    private void spawnHitParticles(ServerLevel level, LivingEntity target) {
        Vec3 pos = target.position();
        for (int i = 0; i < 30; i++) {
            double angle = Math.toRadians(i * 12);
            double h = i * 0.1D;
            double r = 1.5D - (i * 0.04D);
            level.sendParticles(ParticleTypes.WITCH, pos.x + Math.cos(angle) * r, pos.y + h, pos.z + Math.sin(angle) * r, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.SOUL, pos.x + Math.cos(angle) * r, pos.y + h, pos.z + Math.sin(angle) * r, 1, 0, 0, 0, 0);
        }
    }

    @Override
    protected Item getDefaultItem() {
        return Items.AIR; // 見た目はレンダラーで制御するのでAIRでOK
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            this.discard(); // 何かに当たったら消滅
        }
    }
}
