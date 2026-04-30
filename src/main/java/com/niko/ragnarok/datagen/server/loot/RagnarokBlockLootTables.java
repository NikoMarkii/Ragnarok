package com.niko.ragnarok.datagen.server.loot;

import com.niko.ragnarok.block.RagnarokBlocks;
import com.niko.ragnarok.item.Ragnarok_mainItems;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

import java.util.Set;

public class RagnarokBlockLootTables extends BlockLootSubProvider {
    protected RagnarokBlockLootTables() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    protected void generate() {
        this.dropSelf(RagnarokBlocks.NAITOMEA_BLOCK.get());
        this.dropSelf(RagnarokBlocks.RAW_NAITOMEA_BLOCK.get());
        this.add(RagnarokBlocks.NAITOMEA_ORE.get(),
                block -> this.createOreDrop(block, Ragnarok_mainItems.RAW_NAITOMEA.get()));

    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return RagnarokBlocks.BLOCKS.getEntries().stream().map(RegistryObject::get)::iterator;
    }
}
