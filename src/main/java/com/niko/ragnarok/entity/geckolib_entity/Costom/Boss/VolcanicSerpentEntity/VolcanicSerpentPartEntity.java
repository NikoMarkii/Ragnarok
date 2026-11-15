package com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.VolcanicSerpentEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** BodyとTailに共通する、頭部の移動履歴をなぞる追従処理。 */
public abstract class VolcanicSerpentPartEntity extends VolcanicSerpentBaseEntity {
    private UUID parentUUID;
    private double followDistance;

    protected VolcanicSerpentPartEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public void setParentUUID(UUID parentUUID) {
        this.parentUUID = parentUUID;
    }

    public UUID getParentUUID() {
        return this.parentUUID;
    }

    public void setFollowDistance(double followDistance) {
        this.followDistance = Math.max(0.0D, followDistance);
    }

    public double getFollowDistance() {
        return this.followDistance;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide || this.parentUUID == null) return;
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        Entity entity = serverLevel.getEntity(this.parentUUID);
        if (!(entity instanceof VolcanicSerpentHeadEntity head) || !head.isAlive()) return;

        Vec3 target = head.getPositionAtDistance(this.followDistance);
        float targetYaw = head.getYawAtDistance(this.followDistance);
        float targetPitch = head.getPitchAtDistance(this.followDistance);

        // 履歴側ですでに滑らかな軌跡を作っているため、サーバー上では位置を直接合わせる。
        // クライアント側の通常のEntity補間が描画を滑らかにする。
        this.setPos(target.x, target.y, target.z);
        this.setYRot(targetYaw);
        this.setXRot(targetPitch);
        this.yRotO = targetYaw;
        this.xRotO = targetPitch;
        this.setSerpentRoll(head.getSerpentRoll());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.parentUUID != null) tag.putUUID("ParentUUID", this.parentUUID);
        tag.putDouble("FollowDistance", this.followDistance);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.parentUUID = tag.hasUUID("ParentUUID") ? tag.getUUID("ParentUUID") : null;
        this.followDistance = tag.getDouble("FollowDistance");
    }
}
