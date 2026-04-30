package com.niko.ragnarok.datagen.client;

import com.niko.ragnarok.block.RagnarokBlocks;
import com.niko.ragnarok.Ragnarok;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

public class RagnarokBlockStateProvider extends BlockStateProvider {
    public RagnarokBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, Ragnarok.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        simpleBlockWithItem(RagnarokBlocks.NAITOMEA_BLOCK);
        simpleBlockWithItem(RagnarokBlocks.RAW_NAITOMEA_BLOCK);
        simpleBlockWithItem(RagnarokBlocks.NAITOMEA_ORE);

    }

    private void simpleBlockWithItem(RegistryObject<Block> block) {
        simpleBlockWithItem(block.get(),cubeAll(block.get()));
    }
}
