package com.niko.ragnarok.datagen;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.datagen.client.ENJPLanguageProvider;
import com.niko.ragnarok.datagen.client.ENUSLanguageProvider;
import com.niko.ragnarok.datagen.client.RagnarokBlockStateProvider;
import com.niko.ragnarok.datagen.client.RagnarokItemsModelProvider;
import com.niko.ragnarok.datagen.server.RagnarokCuriosSlotProvider;
import com.niko.ragnarok.datagen.server.RagnarokGlobalLootModfierProvider;
import com.niko.ragnarok.datagen.server.RagnarokRecipeProvider;
import com.niko.ragnarok.datagen.server.RagnarokWorldGenProvider;
import com.niko.ragnarok.datagen.server.loot.RagnarokLootTables;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = Ragnarok.MOD_ID,bus = Mod.EventBusSubscriber.Bus.MOD)
public class RagnarokDataGenerators {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator Generator = event.getGenerator();
        PackOutput packOutput = Generator.getPackOutput();
        ExistingFileHelper existingFileHelper =event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookUpProvider = event.getLookupProvider();

        Generator.addProvider(event.includeClient(),new RagnarokItemsModelProvider(packOutput,
                existingFileHelper));
        Generator.addProvider(event.includeClient(),new RagnarokBlockStateProvider(packOutput,
                existingFileHelper));
        Generator.addProvider(event.includeClient(),new ENUSLanguageProvider(packOutput));
        Generator.addProvider(event.includeClient(),new ENJPLanguageProvider(packOutput));
        Generator.addProvider(event.includeServer(), new RagnarokBlockTagsProvider(packOutput,
                lookUpProvider,existingFileHelper));
        Generator.addProvider(event.includeServer(),new RagnarokRecipeProvider(packOutput));
        Generator.addProvider(event.includeServer(), RagnarokLootTables.create(packOutput));
        Generator.addProvider(event.includeServer(), new RagnarokWorldGenProvider(packOutput,lookUpProvider));
        Generator.addProvider(event.includeServer(),
                new RagnarokGlobalLootModfierProvider(packOutput));
        Generator.addProvider(event.includeServer(), new RagnarokCuriosSlotProvider(packOutput));
    }
}
