package com.niko.ragnarok.event;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.config.RagnarokConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Ragnarok.MOD_ID)
public final class BossConfigEvents {
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity living)) return;
        RagnarokConfig.Stats stats = RagnarokConfig.forEntity(living);
        if (stats != null) stats.apply(living);
    }
}
