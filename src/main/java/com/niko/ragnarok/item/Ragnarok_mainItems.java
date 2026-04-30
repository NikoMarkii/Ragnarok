package com.niko.ragnarok.item;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.RagnarokEntities;
import net.minecraft.world.item.*;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.awt.*;

public class Ragnarok_mainItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Ragnarok.MOD_ID);

    public static final RegistryObject<Item> NAITOMEA_INGOD = ITEMS.register("naitomea_ingod", ItemNaitomeaIngod::new);

    public static final RegistryObject<Item> RAW_NAITOMEA = ITEMS.register("raw_naitomea", ItemRawNaitomea::new);

    public static final RegistryObject<Item> DRAGON_SCALE = ITEMS.register("dragon_scale", ItemDragonScale::new);

    public static final RegistryObject<Item> SCORPION_CELL = ITEMS.register("scorpion_cell", ItemScorpionCell::new);

    public static final RegistryObject<Item> SCORPION_NEEDLE = ITEMS.register("scorpion_needle", ItemScorpionNeedle::new);

    public static final RegistryObject<Item> SCORPION_NECKLACE = ITEMS.register("scorpion_necklace",ItemScorpionNecklace::new);

    public static final RegistryObject<Item> RED_CREEPER_SPAWN_EGG = ITEMS.register("red_creeper_spawn_egg",
            () -> new ForgeSpawnEggItem(RagnarokEntities.RED_CREEPER,
                    Color.RED.getRGB(),Color.BLACK.getRGB(),new Item.Properties()));

    public static final RegistryObject<Item> SCORPION_SPAWN_EGG = ITEMS.register("scorpion_spawn_egg",
            () -> new ForgeSpawnEggItem(RagnarokEntities.SCORPION,
                    Color.DARK_GRAY.getRGB(),Color.RED.getRGB(),new Item.Properties()));

    public static final RegistryObject<Item> T_LEX_SPAWN_EGG = ITEMS.register("t_lex_spawn_egg",
            () -> new ForgeSpawnEggItem(RagnarokEntities.T_LEX,
                    Color.GREEN.getRGB(),Color.RED.getRGB(),new Item.Properties()));

    public static final RegistryObject<Item> GROOT_SPAWN_EGG = ITEMS.register("groot_spawn_egg",
            () -> new ForgeSpawnEggItem(RagnarokEntities.GROOT,
                    Color.lightGray.getRGB(),Color.GREEN.getRGB(),new Item.Properties()));

    public static final RegistryObject<Item> NAITOMEA_SWORD = ITEMS.register("naitomea_sword",
            () -> new SwordItem(RagnarokTooltiers.NAITOMEA_TOOL,4,-2.4F,new Item.Properties()));
    public static final RegistryObject<Item> NAITOMEA_PICKAXE = ITEMS.register("naitomea_pickaxe",
            () -> new PickaxeItem(RagnarokTooltiers.NAITOMEA_TOOL,1,-2.8F,new Item.Properties()));
    public static final RegistryObject<Item> NAITOMEA_AXE = ITEMS.register("naitomea_axe",
            () -> new AxeItem(RagnarokTooltiers.NAITOMEA_TOOL,5.0F,-3.0F,new Item.Properties()));
    public static final RegistryObject<Item> NAITOMEA_SHOVEL = ITEMS.register("naitomea_shovel",
            () -> new ShovelItem(RagnarokTooltiers.NAITOMEA_TOOL,1.5F,-3.0F,new Item.Properties()));
    public static final RegistryObject<Item> NAITOMEA_HOE = ITEMS.register("naitomea_hoe",
            () -> new HoeItem(RagnarokTooltiers.NAITOMEA_TOOL,-4,0.0F,new Item.Properties()));
}
