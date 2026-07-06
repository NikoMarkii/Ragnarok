package com.niko.ragnarok.entity.Projectile;

import com.niko.ragnarok.entity.RagnarokEntities;
import com.niko.ragnarok.item.Ragnarok_mainItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class GlowingDustEntity extends ThrowableItemProjectile {
    public GlowingDustEntity(EntityType<? extends GlowingDustEntity> type, Level level) {
        super(type, level);
    }
    @Override
    public void tick() {
        // 1. 位置の更新（基本）
        Vec3 delta = this.getDeltaMovement();
        double nextX = this.getX() + delta.x;
        double nextY = this.getY() + delta.y;
        double nextZ = this.getZ() + delta.z;
        this.updateRotation();

        // 2. 衝突判定
        HitResult hitresult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitresult.getType() != HitResult.Type.MISS) {
            this.onHit(hitresult);
        }

        // 3. 座標の更新
        this.setPos(nextX, nextY, nextZ);
        this.checkInsideBlocks();

        // ★ 4. 重力の適用（ここがポイント！）
        // 雪玉と同じように、毎チック少しずつ下方向への速度を加速させるんだ
        if (!this.isNoGravity()) {
            Vec3 vec3 = this.getDeltaMovement();
            // 0.03は雪玉や卵と同じくらいの重力加速度だよ
            this.setDeltaMovement(vec3.x, vec3.y - 0.03D, vec3.z);
        }

        // 5. パーティクル演出（飛んでいる間のキラキラ）
        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.END_ROD,
                    this.getX(), this.getY() + 0.1, this.getZ(), 0, 0, 0);
        }
    }
    public GlowingDustEntity(Level level, LivingEntity shooter) {
        super(RagnarokEntities.GLOWING_DUST_PROJECTILE.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return Ragnarok_mainItems.GLOWING_DUST.get();
    }

    @Override
    protected void onHit(HitResult result) {

        if (!this.level().isClientSide) {
            // 周囲のエンティティへの弱体化処理
            AABB area = this.getBoundingBox().inflate(4.0D);
            List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class, area);

            for (LivingEntity entity : entities) {
                if (entity instanceof Enemy) {
                    entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
                    entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
                }
            }

            // 演出用のパーティクル（ニコ氏が望むキラキラ）
            ((ServerLevel)this.level()).sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 20, 0.5, 0.5, 0.5, 0.05);

            // 最後に discard するなら、super.onHit は呼ばなくても良い
            this.discard();
        }
    }
}
