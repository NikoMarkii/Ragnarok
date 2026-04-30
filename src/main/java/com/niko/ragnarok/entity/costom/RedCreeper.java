package com.niko.ragnarok.entity.costom;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class RedCreeper extends Creeper {
    public RedCreeper(EntityType<? extends Creeper> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttribute() {
        return Monster.createMonsterAttributes().add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    public static boolean canSpawn(EntityType<RedCreeper> entityType, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos position, RandomSource random) {
        return Monster.checkMonsterSpawnRules(entityType, level, spawnType, position, random);
    }

    @Override
    public void tick() {
        super.tick();

        // サーバー側で爆発直前のタイミングを検知
        if (!this.level().isClientSide && this.isAlive()) {
            // クリーパーの膨らみが最大（爆発直前）になったら自前の爆発を呼ぶ
            if (this.getSwelling(1.0F) >= 1.0F) {
                this.explodeRedCreeper();
            }
        }
        if (this.level().isClientSide && this.isAlive()) {
            if (this.getSwelling(1.0F) > 0.5F) {
                this.level().addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,
                        this.getRandomX(0.5D), this.getRandomY(), this.getRandomZ(0.5D), 0.0D, 0.0D, 0.0D);
            }
        }
    }

    private void explodeRedCreeper() {
        if (!this.level().isClientSide) {
            // 爆発の威力（帯電状態ならバニラ同様に倍加）
            float f = this.isPowered() ? 6.0F : 3.0F;
            this.dead = true;

            // 1. 物理的な爆発（ブロック破壊とダメージ）
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), f, Level.ExplosionInteraction.MOB);

            // 2. プレイヤーを燃やす処理（爆発範囲内の全エンティティが対象）
            AABB area = this.getBoundingBox().inflate(f);
            List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class, area);
            for (LivingEntity entity : entities) {
                // 自分自身は除外し、視線が通る（爆風が届く）相手を8秒間燃やす
                if (entity != this && this.hasLineOfSight(entity)) {
                    entity.setSecondsOnFire(8);
                }
            }

            // 3. 周囲の地面を燃やす処理
            int radius = Mth.floor(f);
            for (BlockPos blockpos : BlockPos.betweenClosed(this.blockPosition().offset(-radius, -1, -radius), this.blockPosition().offset(radius, 1, radius))) {
                // 3分の1の確率で地面に火を放つ
                if (this.random.nextInt(3) == 0 && this.level().getBlockState(blockpos).isAir() &&
                        this.level().getBlockState(blockpos.below()).isSolidRender(this.level(), blockpos.below())) {
                    this.level().setBlockAndUpdate(blockpos, BaseFireBlock.getState(this.level(), blockpos));
                }
            }

            this.discard();
        }
    }
}