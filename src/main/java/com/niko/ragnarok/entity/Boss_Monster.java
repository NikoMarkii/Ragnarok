package com.niko.ragnarok.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public abstract class Boss_Monster extends Monster {

    protected Boss_Monster(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    // ピースフルでもデスポーンしない
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    // デスポーン処理を無効化
    @Override
    public void checkDespawn() {
        // 何もしない
    }
}
