package com.niko.ragnarok.worldgen.features;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.block.RagnarokBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

import java.util.List;

public class RagnarokOreFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>> NAITOMEA_ORE_KEY =
            createKey("naitomea_ore");

    public static void bootstrap(BootstapContext<ConfiguredFeature<?, ?>> context) {
        RuleTest endReplaceables = new BlockMatchTest(Blocks.END_STONE);

        List<OreConfiguration.TargetBlockState> naitomeaOres = List.of(
                OreConfiguration.target(endReplaceables,
                        RagnarokBlocks.NAITOMEA_ORE.get().defaultBlockState())
        );
        FeatureUtils.register(context,NAITOMEA_ORE_KEY, Feature.ORE,
                new OreConfiguration(naitomeaOres, 9));
    }

    public static ResourceKey<ConfiguredFeature<?,?>>
                  createKey(String name){
        return ResourceKey.create(Registries.CONFIGURED_FEATURE,
                ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID,name));
    }

}
