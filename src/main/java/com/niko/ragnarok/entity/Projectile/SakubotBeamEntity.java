package com.niko.ragnarok.entity.Projectile;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public class SakubotBeamEntity extends ThrowableItemProjectile {
    public SakubotBeamEntity(EntityType<? extends SakubotBeamEntity> type, Level level) {
        super(type, level);
    }
public SakubotBeamEntity(EntityType<? extends SakubotBeamEntity> type, LivingEntity shooter, Level level) {
        super(type, shooter, level);
}
    @Override
    protected Item getDefaultItem() {
        return Items.AIR;
    }
@Override
    protected float getGravity() {
        return 0.0F;
}
@Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            for (int i = 0; i < 4; i++) {
                double factor = i / 4.0D;
                double px =this.getX() - this.getDeltaMovement().x * factor;
                double py = this.getY() - this.getDeltaMovement().y * factor;
                double pz = this.getZ() - this.getDeltaMovement().z * factor;
            }
        }
        if (this.tickCount > 40) {
            this.discard();
        }
    }
    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide) {
            if (!(result.getEntity() != this.getOwner())) {
                result.getEntity().hurt(this.damageSources().mobAttack((LivingEntity) this.getOwner()), 12.0F );

                this.level().explode(this, this.getX(), this.getY(), this.getZ(), 1.0F, Level.ExplosionInteraction.NONE);
                this.discard();
            }
        }
    }
    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level().isClientSide) {
            this.discard();
        }
    }
}
