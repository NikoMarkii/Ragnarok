package com.niko.ragnarok.datagen.server;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.worldgen.biome.RagnarokBiomeModifiers;
import com.niko.ragnarok.worldgen.features.RagnarokOreFeatures;
import com.niko.ragnarok.worldgen.placement.RagnarokOreplacement;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
public class RagnarokWorldGenProvider extends DatapackBuiltinEntriesProvider{
    private static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.CONFIGURED_FEATURE, RagnarokOreFeatures::bootstrap)
            .add(Registries.PLACED_FEATURE, RagnarokOreplacement::bootstrap)
            .add(ForgeRegistries.Keys.BIOME_MODIFIERS, RagnarokBiomeModifiers::bootstrap);

    public RagnarokWorldGenProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, BUILDER, Set.of(Ragnarok.MOD_ID));
    }
}
