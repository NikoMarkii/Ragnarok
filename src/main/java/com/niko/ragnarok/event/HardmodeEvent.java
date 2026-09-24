package com.niko.ragnarok.event;

import com.niko.ragnarok.world.WorldModeData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "ragnarok")
public class HardmodeEvent {

    /**
     * エンティティがワールドに参加した時の処理
     */
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        // サーバー側かつスポーンしたのがクリーパーの場合のみ処理
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof Creeper creeper) {

            // ハードモード以上の判定
            WorldModeData modeData = WorldModeData.get(level);
            if (modeData.getCurrentState().getId() >= WorldModeData.GameModeState.HARD.getId()) {

                // NBT経由でクリーパーの帯電状態（powered）を付与
                if (level.random.nextFloat() < 0.40F) {
                    CompoundTag tag = new CompoundTag();
                    creeper.addAdditionalSaveData(tag);
                    tag.putBoolean("powered", true);
                    creeper.readAdditionalSaveData(tag);
                }
            }
        }
    }
}