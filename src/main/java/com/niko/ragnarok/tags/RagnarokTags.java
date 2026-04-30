package com.niko.ragnarok.tags;
import com.niko.ragnarok.Ragnarok;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class RagnarokTags {
    public static class Blocks {
        public static final TagKey<Block> NAITOMEA_SERIES = tag("naitomea_series");
        public static final TagKey<Block> NAITOMEA_TOOL = tag("naitomea_tool");


        private static TagKey<Block> tag(String name) {
            return BlockTags.create(new ResourceLocation(Ragnarok.MOD_ID, name));
        }
    }

    public static class Items {

        private static TagKey<Item> tag(String name) {
            return ItemTags.create(new ResourceLocation(Ragnarok.MOD_ID, name));
        }
    }
}