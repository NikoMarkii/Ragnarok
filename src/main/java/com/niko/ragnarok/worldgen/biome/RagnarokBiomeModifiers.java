package com.niko.ragnarok.worldgen.biome;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.RagnarokEntities;
import com.niko.ragnarok.worldgen.placement.RagnarokOreplacement;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ForgeBiomeModifiers;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public class RagnarokBiomeModifiers {
    public static final ResourceKey<BiomeModifier> ADD_NAITOMEA_ORE =
            createKey("add_naitomea_ore");
    public static final ResourceKey<BiomeModifier> SPAWN_RED_CREEPER =
            createKey("spawn_red_creeper");
    public static final ResourceKey<BiomeModifier> SPAWN_SCORPION =
            createKey("spawn_scorpion");

    public static void bootstrap(BootstapContext<BiomeModifier> context) {
        HolderGetter<PlacedFeature> placedFeatures =
                context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);

        context.register(ADD_NAITOMEA_ORE, new ForgeBiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_END),
                HolderSet.direct(placedFeatures.getOrThrow(RagnarokOreplacement.NAITOMEA_ORE)),
                GenerationStep.Decoration.UNDERGROUND_ORES));

        context.register(SPAWN_RED_CREEPER, new ForgeBiomeModifiers.AddSpawnsBiomeModifier(
                HolderSet.direct(biomes.getOrThrow(Biomes.CRIMSON_FOREST), biomes.getOrThrow(Biomes.WARPED_FOREST)),
                List.of(new MobSpawnSettings.SpawnerData(RagnarokEntities.RED_CREEPER.get(), 25, 1, 3))));

        context.register(SPAWN_SCORPION, new ForgeBiomeModifiers.AddSpawnsBiomeModifier(
                HolderSet.direct(biomes.getOrThrow(Biomes.JUNGLE), biomes.getOrThrow(Biomes.SAVANNA),
                        biomes.getOrThrow(Biomes.DESERT),biomes.getOrThrow(Biomes.BAMBOO_JUNGLE),
                        biomes.getOrThrow(Biomes.SPARSE_JUNGLE),
                        biomes.getOrThrow(Biomes.PLAINS)),
                List.of(new MobSpawnSettings.SpawnerData(RagnarokEntities.SCORPION.get(), 25, 1, 3))));
    }

    private static ResourceKey<BiomeModifier> createKey(String name) {
        return ResourceKey.create(ForgeRegistries.Keys.BIOME_MODIFIERS,
                ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, name));
    }
}
