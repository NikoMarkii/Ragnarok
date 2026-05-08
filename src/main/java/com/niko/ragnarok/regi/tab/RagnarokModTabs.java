package com.niko.ragnarok.regi.tab;

import com.niko.ragnarok.block.RagnarokBlocks;
import com.niko.ragnarok.item.Ragnarok_mainItems;
import com.niko.ragnarok.Ragnarok;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class RagnarokModTabs {

    public static final DeferredRegister<CreativeModeTab> MOD_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Ragnarok.MOD_ID);

    public static final RegistryObject<CreativeModeTab> RAGNAROK_MAIN_MAIN = MOD_TABS.register("ragnarok_main",
            () -> {return CreativeModeTab.builder()
                        .icon(() -> new ItemStack(Ragnarok_mainItems.NAITOMEA_INGOD.get()))
                        .title(Component.translatable("itemGroup.ragnarok_main"))
                        .displayItems((paran, output) -> {
                            for (Item item : ragnarokmain.items) {
                                output.accept(Ragnarok_mainItems.VOID_SCYTHE.get());
                                output.accept(Ragnarok_mainItems.NAITOMEA_SWORD.get());
                                output.accept(Ragnarok_mainItems.NAITOMEA_PICKAXE.get());
                                output.accept(Ragnarok_mainItems.NAITOMEA_AXE.get());
                                output.accept(Ragnarok_mainItems.NAITOMEA_SHOVEL.get());
                                output.accept(Ragnarok_mainItems.NAITOMEA_HOE.get());
                                output.accept(Ragnarok_mainItems.SCORPION_NECKLACE.get());
                                output.accept(Ragnarok_mainItems.NAITOMEA_INGOD.get());
                                output.accept(RagnarokBlocks.NAITOMEA_BLOCK.get());
                                output.accept(Ragnarok_mainItems.RAW_NAITOMEA.get());
                                output.accept(RagnarokBlocks.RAW_NAITOMEA_BLOCK.get());
                                output.accept(RagnarokBlocks.NAITOMEA_ORE.get());
                                output.accept(Ragnarok_mainItems.DRAGON_SCALE.get());
                                output.accept(Ragnarok_mainItems.ENDER_SOLDIER_CLAW.get());
                                output.accept(Ragnarok_mainItems.GROOT_HARHT.get());
                                output.accept(Ragnarok_mainItems.SCORPION_NEEDLE.get());
                                output.accept(Ragnarok_mainItems.SCORPION_CELL.get());
                                output.accept(Ragnarok_mainItems.ENDER_SOLDIER_SPAWN_EGG.get());
                                output.accept(Ragnarok_mainItems.T_LEX_SPAWN_EGG.get());
                                output.accept(Ragnarok_mainItems.GROOT_SPAWN_EGG.get());
                                output.accept(Ragnarok_mainItems.MINI_GROOT_SPAWN_EGG.get());
                                output.accept(Ragnarok_mainItems.RED_CREEPER_SPAWN_EGG.get());
                                output.accept(Ragnarok_mainItems.MAGIC_GOLEM_SPAWN_EGG.get());
                                output.accept(Ragnarok_mainItems.SCORPION_SPAWN_EGG.get());
                            }
                        })
                        .build();
            });
}
