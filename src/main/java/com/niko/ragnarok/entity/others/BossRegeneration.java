package com.niko.ragnarok.entity.others;

import com.niko.ragnarok.config.RagnarokConfig;
import net.minecraft.world.entity.Mob;

public final class BossRegeneration {
    private int timer;

    public void tick(Mob mob, boolean standby, float fallbackAmount) {
        if (mob.level().isClientSide) return;
        RagnarokConfig.Stats stats = RagnarokConfig.forEntity(mob);
        float amount = stats == null ? fallbackAmount : stats.regenAmount.get().floatValue();
        int interval = stats == null ? 20 : stats.regenIntervalTicks.get();
        boolean eligible = amount > 0 && !standby && mob.isAlive() && !mob.isDeadOrDying()
                && mob.getHealth() < mob.getMaxHealth()
                && (mob.getTarget() == null || !mob.getTarget().isAlive());
        if (advance(eligible, interval)) mob.heal(amount);
    }

    boolean advance(boolean eligible, int interval) {
        if (!eligible) {
            timer = 0;
            return false;
        }
        if (++timer >= interval) {
            timer = 0;
            return true;
        }
        return false;
    }
}
