package com.niko.ragnarok.event;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.RagnarokEntities;
import com.niko.ragnarok.entity.costom.*;
import com.niko.ragnarok.entity.geckolib_entity.Costom.Ender_Soldier;
import com.niko.ragnarok.entity.geckolib_entity.Costom.Fairy;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.niko.ragnarok.entity.geckolib_entity.Costom.Fairy.isBrightEnoughToSpawn;

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
        event.put(RagnarokEntities.MAGIC_GOLEM.get(),
                Magic_Golem.createAttributes().build());
        event.put(RagnarokEntities.ENDER_SOLDIER.get(),
                Ender_Soldier.createAttributes().build());
        event.put(RagnarokEntities.FAIRY.get(),
                Fairy.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
      event.register(RagnarokEntities.RED_CREEPER.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.WORLD_SURFACE,
              Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);

        event.register(RagnarokEntities.ENDER_SOLDIER.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.WORLD_SURFACE,
                Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);

        event.register(RagnarokEntities.GROOT.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Groot::checkGrootSpawnRules, // ここを自前のメソッドに変更！
                SpawnPlacementRegisterEvent.Operation.REPLACE);

        event.register(RagnarokEntities.T_LEX.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (entityType, level, spawnType, pos, random) -> {
                    // 水の上ではないこと ＋ Animalとしてのスポーンルール
                    return level.getFluidState(pos.below()).isEmpty() &&
                            Animal.checkAnimalSpawnRules(entityType, level, spawnType, pos, random);
                },
                SpawnPlacementRegisterEvent.Operation.REPLACE);

        event.register(RagnarokEntities.FAIRY.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (entityType, level, spawnType, pos, random) -> {
                    // 1. 水の上ではないこと
                    // 2. Mobとしての一般的なスポーンルール（地面が適切かなど）を満たすこと
                    // 3. 明るさのチェック（動物のように明るい場所だけにしたい場合）
                    return level.getFluidState(pos.below()).isEmpty() &&
                            Mob.checkMobSpawnRules(entityType, level, spawnType, pos, random) &&
                            isBrightEnoughToSpawn(level, pos); // 明るさチェックを足すなら
                },
                SpawnPlacementRegisterEvent.Operation.REPLACE);

        // スニッファーの登録
        event.register(EntityType.SNIFFER,
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (entityType, level, spawnType, pos, random) -> {
                    // 水の上ではないこと ＋ Animalとしてのスポーンルール
                    return level.getFluidState(pos.below()).isEmpty() &&
                            Animal.checkAnimalSpawnRules(entityType, level, spawnType, pos, random);
                },
                SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
}

