package com.niko.ragnarok;

import com.niko.ragnarok.block.RagnarokBlocks;
import com.niko.ragnarok.entity.RagnarokEntities;
import com.niko.ragnarok.item.Ragnarok_mainItems;
import com.niko.ragnarok.loot.RagnarokLootModifiers;
import com.niko.ragnarok.regi.tab.RagnarokModTabs;
import com.niko.ragnarok.sound.RagnarokSoundEvents;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("ragnarok")
public class Ragnarok {
        public static final String MOD_ID = "ragnarok";
        public Ragnarok(){

                IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
                Ragnarok_mainItems.ITEMS.register(bus);
                RagnarokModTabs.MOD_TABS.register(bus);
                RagnarokBlocks.BLOCKS.register(bus);
                RagnarokLootModifiers.register(bus);
                RagnarokEntities.register(bus);
                RagnarokSoundEvents.SOUND_EVENTS.register(bus);

        }
}
