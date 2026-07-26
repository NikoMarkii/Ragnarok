package com.niko.ragnarok.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class Mid_Boss_Monster extends Monster {
    protected Mid_Boss_Monster(EntityType<? extends Monster> p_33002_, Level p_33003_) {
        super(p_33002_, p_33003_);
    }
    @Override
    public void checkDespawn() {
        // デスポーン処理を無効化しつつ、AIがサボらないようにアイドル時間をリセットし続ける
        this.noActionTime = 0;
    }
    @Override
    public boolean causeFallDamage(float d, float m, DamageSource s) {
        return false;
    }
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }
}
