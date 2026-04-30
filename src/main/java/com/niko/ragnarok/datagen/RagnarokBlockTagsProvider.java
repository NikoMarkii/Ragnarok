package com.niko.ragnarok.datagen;

import com.niko.ragnarok.block.RagnarokBlocks;
import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.tags.RagnarokTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class RagnarokBlockTagsProvider extends BlockTagsProvider {
    public RagnarokBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Ragnarok.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider p_256380_) {
        this.tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(RagnarokBlocks.NAITOMEA_BLOCK.get(),
                        RagnarokBlocks.RAW_NAITOMEA_BLOCK.get(),
                        RagnarokBlocks.NAITOMEA_ORE.get());
        this.tag(Tags.Blocks.NEEDS_NETHERITE_TOOL).add(RagnarokBlocks.NAITOMEA_BLOCK.get());
        this.tag(Tags.Blocks.NEEDS_NETHERITE_TOOL).add(RagnarokBlocks.RAW_NAITOMEA_BLOCK.get());
        this.tag(Tags.Blocks.NEEDS_NETHERITE_TOOL).add(RagnarokBlocks.NAITOMEA_ORE.get());
        this.tag(RagnarokTags.Blocks.NAITOMEA_TOOL).add(RagnarokBlocks.NAITOMEA_BLOCK.get());
        this.tag(RagnarokTags.Blocks.NAITOMEA_TOOL).add(RagnarokBlocks.NAITOMEA_ORE.get());
        this.tag(RagnarokTags.Blocks.NAITOMEA_TOOL).add(RagnarokBlocks.RAW_NAITOMEA_BLOCK.get());
        this.tag(RagnarokTags.Blocks.NAITOMEA_TOOL).add(Blocks.DIAMOND_ORE);
        this.tag(RagnarokTags.Blocks.NAITOMEA_TOOL).add(Blocks.ANCIENT_DEBRIS);
        this.tag(RagnarokTags.Blocks.NAITOMEA_TOOL).add(Blocks.NETHERITE_BLOCK);
        this.tag(RagnarokTags.Blocks.NAITOMEA_TOOL).add(Blocks.DIAMOND_BLOCK);

    }
}
