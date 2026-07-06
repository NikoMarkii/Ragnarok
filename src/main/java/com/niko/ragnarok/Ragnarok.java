package com.niko.ragnarok;

import com.niko.ragnarok.block.RagnarokBlocks;
import com.niko.ragnarok.effect.ModMobEffects;
import com.niko.ragnarok.entity.RagnarokEntities;
import com.niko.ragnarok.item.Ragnarok_mainItems;
import com.niko.ragnarok.loot.RagnarokLootModifiers;
import com.niko.ragnarok.regi.tab.RagnarokModTabs;
import com.niko.ragnarok.sound.RagnarokSoundEvents;
import com.niko.ragnarok.worldgen.biome.RagnarokBiomes;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import software.bernie.geckolib.GeckoLib;

@Mod("ragnarok")
public class Ragnarok {
        public static final String MOD_ID = "ragnarok";
        public Ragnarok(FMLJavaModLoadingContext context) {

                IEventBus bus = context.getModEventBus();

                ModMobEffects.MOB_EFFECTS.register(bus);
                Ragnarok_mainItems.ITEMS.register(bus);
                RagnarokModTabs.MOD_TABS.register(bus);
                RagnarokBlocks.BLOCKS.register(bus);
                RagnarokLootModifiers.register(bus);
                RagnarokEntities.register(bus);
                RagnarokSoundEvents.SOUND_EVENTS.register(bus);

                GeckoLib.initialize();
        }
}
