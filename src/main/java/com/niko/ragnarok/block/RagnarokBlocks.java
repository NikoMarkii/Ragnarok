package com.niko.ragnarok.block;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.item.Ragnarok_mainItems;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class RagnarokBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Ragnarok.MOD_ID);
            public static final RegistryObject<Block> NAITOMEA_BLOCK = regiserBlockItem("naitomea_block",
                    () -> new Block(BlockBehaviour.Properties.copy(Blocks.NETHERITE_BLOCK).sound(SoundType.NETHERITE_BLOCK) ));
            public static final RegistryObject<Block> RAW_NAITOMEA_BLOCK = regiserBlockItem("raw_naitomea_block",
                    () -> new Block(BlockBehaviour.Properties.copy(Blocks.NETHERITE_BLOCK).sound(SoundType.NETHERITE_BLOCK) ));
            public static final RegistryObject<Block> NAITOMEA_ORE = regiserBlockItem("naitomea_ore",
                    () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.ANCIENT_DEBRIS).sound(SoundType.NETHERITE_BLOCK),
                          UniformInt.of(3, 7)));
            private static <T extends Block> RegistryObject<T> regiserBlockItem(String name,
                                                                                Supplier<T> Supplier) {
                RegistryObject<T> block = BLOCKS.register(name,Supplier);
                Ragnarok_mainItems.ITEMS.register(name,() -> new BlockItem(block.get(),new Item.Properties()));
                return block;
            }

            public static void resister(IEventBus eventBus) {
                BLOCKS.register(eventBus);
            }
}
