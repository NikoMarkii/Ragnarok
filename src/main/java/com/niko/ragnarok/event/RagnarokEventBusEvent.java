package com.niko.ragnarok.event;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.RagnarokEntities;
import com.niko.ragnarok.entity.costom.*;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Ragnarok.MOD_ID,bus = Mod.EventBusSubscriber.Bus.MOD)
public class RagnarokEventBusEvent {
    @SubscribeEvent
    public static void registerAttributes(
            EntityAttributeCreationEvent event) {
        event.put(RagnarokEntities.RED_CREEPER.get(),
                RedCreeper.createMobAttributes().build());
        event.put(RagnarokEntities.SCORPION.get(),
                Scorpion.createMobAttributes().build());
        event.put(RagnarokEntities.T_LEX.get(),
                TLex.createMobAttributes().build());
        event.put(RagnarokEntities.GROOT.get(),
                Groot.createAttributes().build());
        event.put(RagnarokEntities.MINI_GROOT.get(),
                Mini_Groot.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
      event.register(RagnarokEntities.RED_CREEPER.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.WORLD_SURFACE,
              Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);

        event.register(RagnarokEntities.GROOT.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Groot::checkGrootSpawnRules, // ここを自前のメソッドに変更！
                SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
}

