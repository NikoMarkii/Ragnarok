package com.niko.ragnarok.datagen.client;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.item.Ragnarok_mainItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

public class RagnarokItemsModelProvider extends ItemModelProvider {
    public RagnarokItemsModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Ragnarok.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        withExistingParent(Ragnarok_mainItems.RED_CREEPER_SPAWN_EGG.getId().getPath(),
                mcLoc("item/template_spawn_egg"));
        withExistingParent(Ragnarok_mainItems.SCORPION_SPAWN_EGG.getId().getPath(),
                mcLoc("item/template_spawn_egg"));
        withExistingParent(Ragnarok_mainItems.T_LEX_SPAWN_EGG.getId().getPath(),
                mcLoc("item/template_spawn_egg"));
        basicItem(Ragnarok_mainItems.RAW_NAITOMEA.get());
        basicItem(Ragnarok_mainItems.NAITOMEA_INGOD.get());
        basicItem(Ragnarok_mainItems.DRAGON_SCALE.get());
        basicItem(Ragnarok_mainItems.SCORPION_CELL.get());
        basicItem(Ragnarok_mainItems.SCORPION_NEEDLE.get());
        basicItem(Ragnarok_mainItems.SCORPION_NECKLACE.get());
        handheldItem(Ragnarok_mainItems.NAITOMEA_SWORD);
        handheldItem(Ragnarok_mainItems.NAITOMEA_PICKAXE);
        handheldItem(Ragnarok_mainItems.NAITOMEA_AXE);
        handheldItem(Ragnarok_mainItems.NAITOMEA_SHOVEL);
        handheldItem(Ragnarok_mainItems.NAITOMEA_HOE);

    }
    private ItemModelBuilder handheldItem(RegistryObject<Item> item) {
        return withExistingParent(item.getId().getPath(),
                new ResourceLocation("item/handheld")).texture("layer0",
                new ResourceLocation(Ragnarok.MOD_ID,"item/" + item.getId().getPath()));
    }
}
