package com.niko.ragnarok.item;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.tags.RagnarokTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ForgeTier;
import net.minecraftforge.common.TierSortingRegistry;

import java.util.List;

public class RagnarokTooltiers {
    public static final Tier NAITOMEA_TOOL = TierSortingRegistry.registerTier(
            new ForgeTier(5,2500,10f,6.0f,20,
                    RagnarokTags.Blocks.NAITOMEA_TOOL, () -> Ingredient.of(Ragnarok_mainItems.NAITOMEA_INGOD.get())),
            ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "naitomea_tool"), List.of(Tiers.NETHERITE),List.of());
}
