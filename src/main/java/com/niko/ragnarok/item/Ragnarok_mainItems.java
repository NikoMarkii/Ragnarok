package com.niko.ragnarok.item;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.RagnarokEntities;
import com.niko.ragnarok.item.Armor.NaitomeaArmorItem;
import com.niko.ragnarok.item.Armor.RagnarokArmorMaterials;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.network.chat.Component;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class Ragnarok_mainItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Ragnarok.MOD_ID);

    public static final RegistryObject<Item> NAITOMEA_INGOD = ITEMS.register("naitomea_ingod", ItemNaitomeaIngod::new);

    public static final RegistryObject<Item> RAW_NAITOMEA = ITEMS.register("raw_naitomea", ItemRawNaitomea::new);

    public static final RegistryObject<Item> DRAGON_SCALE = ITEMS.register("dragon_scale", ItemDragonScale::new);

    public static final RegistryObject<Item> ENDER_SOLDIER_CLAW = ITEMS.register("ender_soldier_claw",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> GROOT_HARHT = ITEMS.register("groot_heart", ItemGrootHeart::new);

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
                    0x8b4513,
                    0x008000,
                    new Item.Properties()));

    public static final RegistryObject<Item> MINI_GROOT_SPAWN_EGG = ITEMS.register("mini_groot_spawn_egg",
            () -> new ForgeSpawnEggItem(RagnarokEntities.MINI_GROOT,
                    0x8b4513,
                    0x00ff7f,
                    new Item.Properties()));

    public static final RegistryObject<Item> MAGIC_GOLEM_SPAWN_EGG = ITEMS.register("magic_golem_spawn_egg",
            () -> new ForgeSpawnEggItem(RagnarokEntities.MAGIC_GOLEM,
                    0xCED4D5, // エッグ本体の色
                    0x7F3FB0, // 模様の色
                    new Item.Properties()));

    public static final RegistryObject<Item> ENDER_SOLDIER_SPAWN_EGG = ITEMS.register("ender_soldier_spawn_egg",
            () -> new ForgeSpawnEggItem(RagnarokEntities.ENDER_SOLDIER,
                    0x000000, // エッグ本体の色
                    0x9932cc, // 模様の色
                    new Item.Properties()));

    public static final RegistryObject<Item> NAITOMEA_HELMET = ITEMS.register("naitomea_helmet",
            () -> new NaitomeaArmorItem(RagnarokArmorMaterials.NAITOMEA_ARMOR, ArmorItem.Type.HELMET, new Item.Properties()));

    public static final RegistryObject<Item> NAITOMEA_CHESTPLATE = ITEMS.register("naitomea_chestplate",
            () -> new NaitomeaArmorItem(RagnarokArmorMaterials.NAITOMEA_ARMOR, ArmorItem.Type.CHESTPLATE, new Item.Properties()));

    public static final RegistryObject<Item> NAITOMEA_LEGGINGS = ITEMS.register("naitomea_leggings",
            () -> new NaitomeaArmorItem(RagnarokArmorMaterials.NAITOMEA_ARMOR, ArmorItem.Type.LEGGINGS, new Item.Properties()));

    public static final RegistryObject<Item> NAITOMEA_BOOTS = ITEMS.register("naitomea_boots",
            () -> new NaitomeaArmorItem(RagnarokArmorMaterials.NAITOMEA_ARMOR, ArmorItem.Type.BOOTS, new Item.Properties()));

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

    public static final RegistryObject<Item> NIGHTNIUM_UPGRADE_SMITHING_TEMPLATE = ITEMS.register("nightnium_upgrade_smithing_template",
            () -> new SmithingTemplateItem(
                    // 1. Applies to (適用アイテムの種類)
                    Component.translatable("item.ragnarok.smithing_template.nightnium_upgrade.applies_to").withStyle(ChatFormatting.BLUE),
                    // 2. Ingredients (必要な素材の種類)
                    Component.translatable("item.ragnarok.smithing_template.nightnium_upgrade.ingredients").withStyle(ChatFormatting.BLUE),
                    // 3. Upgrade Title (メニュー上のタイトル: 例 "ナイトニウム強化")
                    Component.translatable("upgrade.ragnarok.nightnium_upgrade").withStyle(ChatFormatting.GRAY),
                    // 4. Base Slot Description (ベーススロットへの指示: 例 "ネザライト装備を入れてください")
                    Component.translatable("item.ragnarok.smithing_template.nightnium_upgrade.base_slot_description"),
                    // 5. Additions Slot Description (追加スロットへの指示: 例 "ナイトニウムインゴットを入れてください")
                    Component.translatable("item.ragnarok.smithing_template.nightnium_upgrade.additions_slot_description"),
                    // 6. Base Slot Icons (シルエットアイコン)
                    Arrays.asList(ResourceLocation.fromNamespaceAndPath("minecraft", "item/empty_slot_sword")),
                    // 7. Additions Slot Icons (素材のシルエットアイコン)
                    Arrays.asList(ResourceLocation.fromNamespaceAndPath("minecraft", "item/empty_slot_ingot"))
            )
    );

    public static final RegistryObject<Item> VOID_SCYTHE = ITEMS.register("void_scythe",
            () -> new VoidScythe(new Item.Properties()
                    .stacksTo(1)
                    .defaultDurability(1500) // 耐久度は高めに設定
                    .rarity(Rarity.RARE))); // レア度を設定
}
