package com.niko.ragnarok.worldgen.biome;

import com.niko.ragnarok.Ragnarok;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

public class RagnarokBiomes {
    // これが古代の森の「住所」になる
    public static final ResourceKey<Biome> ANCIENT_JUNGLE = ResourceKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "ancient_jungle")
    );
}