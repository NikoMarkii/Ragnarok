package com.niko.ragnarok.datagen.server;

import com.niko.ragnarok.block.RagnarokBlocks;
import com.niko.ragnarok.item.Ragnarok_mainItems;
import com.niko.ragnarok.Ragnarok;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.function.Consumer;

public class RagnarokRecipeProvider extends RecipeProvider {
    private static final List<ItemLike> NAITOMEA_SERIES =
            List.of(Ragnarok_mainItems.RAW_NAITOMEA.get(),
                    RagnarokBlocks.NAITOMEA_ORE.get());

    public RagnarokRecipeProvider(PackOutput pOutput) {
        super(pOutput);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> pRecipeOutput) {

        nineBlockStorageRecipes(pRecipeOutput, RecipeCategory.MISC,
                Ragnarok_mainItems.RAW_NAITOMEA.get(),
                RecipeCategory.BUILDING_BLOCKS,
                RagnarokBlocks.RAW_NAITOMEA_BLOCK.get());

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC,
                        Ragnarok_mainItems.NAITOMEA_INGOD.get())
                .pattern("SSS")
                .pattern("S//")
                .pattern("// ")
                .define('S', Ragnarok_mainItems.RAW_NAITOMEA.get())
                .define('/', Ragnarok_mainItems.DRAGON_SCALE.get())
                .unlockedBy(getHasName(Ragnarok_mainItems.RAW_NAITOMEA.get()), has(Ragnarok_mainItems.DRAGON_SCALE.get()))
                .save(pRecipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RagnarokBlocks.NAITOMEA_BLOCK.get())
                .pattern("SSS")
                .pattern("SSS")
                .pattern("SSS")
                .define('S', Ragnarok_mainItems.NAITOMEA_INGOD.get())
                .unlockedBy(getHasName(Ragnarok_mainItems.NAITOMEA_INGOD.get()), has(Ragnarok_mainItems.NAITOMEA_INGOD.get()))
                .save(pRecipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Ragnarok_mainItems.NAITOMEA_INGOD.get(), 9)
                .requires(RagnarokBlocks.NAITOMEA_BLOCK.get())
                .unlockedBy(getHasName(RagnarokBlocks.NAITOMEA_BLOCK.get()), has(RagnarokBlocks.NAITOMEA_BLOCK.get()))
                .save(pRecipeOutput, "naitomea_ingod2");

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC,
                        Ragnarok_mainItems.NAITOMEA_SWORD.get())
                .pattern("   ")
                .pattern(" S ")
                .pattern(" / ")
                .define('S', Ragnarok_mainItems.NAITOMEA_INGOD.get())
                .define('/', Items.NETHERITE_SWORD)
                .unlockedBy(getHasName(Ragnarok_mainItems.NAITOMEA_INGOD.get()), has(Items.NETHERITE_SWORD))
                .save(pRecipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC,
                        Ragnarok_mainItems.NAITOMEA_PICKAXE.get())
                .pattern("   ")
                .pattern(" S ")
                .pattern(" / ")
                .define('S', Ragnarok_mainItems.NAITOMEA_INGOD.get())
                .define('/', Items.NETHERITE_PICKAXE)
                .unlockedBy(getHasName(Ragnarok_mainItems.NAITOMEA_INGOD.get()), has(Items.NETHERITE_PICKAXE))
                .save(pRecipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC,
                        Ragnarok_mainItems.NAITOMEA_AXE.get())
                .pattern("   ")
                .pattern(" S ")
                .pattern(" / ")
                .define('S', Ragnarok_mainItems.NAITOMEA_INGOD.get())
                .define('/', Items.NETHERITE_AXE)
                .unlockedBy(getHasName(Ragnarok_mainItems.NAITOMEA_INGOD.get()), has(Items.NETHERITE_AXE))
                .save(pRecipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC,
                        Ragnarok_mainItems.NAITOMEA_SHOVEL.get())
                .pattern("   ")
                .pattern(" S ")
                .pattern(" / ")
                .define('S', Ragnarok_mainItems.NAITOMEA_INGOD.get())
                .define('/', Items.NETHERITE_SHOVEL)
                .unlockedBy(getHasName(Ragnarok_mainItems.NAITOMEA_INGOD.get()), has(Items.NETHERITE_SHOVEL))
                .save(pRecipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC,
                        Ragnarok_mainItems.NAITOMEA_HOE.get())
                .pattern("   ")
                .pattern(" S ")
                .pattern(" / ")
                .define('S', Ragnarok_mainItems.NAITOMEA_INGOD.get())
                .define('/', Items.NETHERITE_HOE)
                .unlockedBy(getHasName(Ragnarok_mainItems.NAITOMEA_INGOD.get()), has(Items.NETHERITE_HOE))
                .save(pRecipeOutput);
    }
    protected static void oreSmelting(Consumer<FinishedRecipe> pRecipeOutput, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult, float pExperience, int pCookingTime, String pGroup) {
        oreCooking(pRecipeOutput, RecipeSerializer.SMELTING_RECIPE, pIngredients, pCategory, pResult, pExperience, pCookingTime, pGroup, "_from_smelting");
    }
    protected static void oreBlasting(Consumer<FinishedRecipe> pRecipeOutput, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult, float pExperience, int pCookingTime, String pGroup) {
        oreCooking(pRecipeOutput, RecipeSerializer.BLASTING_RECIPE, pIngredients, pCategory, pResult, pExperience, pCookingTime, pGroup, "_from_blasting");
    }
    protected static void netheriteSmithing(Consumer<FinishedRecipe> p_251614_, Item p_250046_, RecipeCategory p_248986_, Item p_250389_) {
        SmithingTransformRecipeBuilder.smithing(Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), Ingredient.of(p_250046_), Ingredient.of(Items.NETHERITE_INGOT), p_248986_, p_250389_).unlocks("has_netherite_ingot", has(Items.NETHERITE_INGOT)).save(p_251614_, getItemName(p_250389_) + "_smithing");
    }

    protected static void oreCooking(Consumer<FinishedRecipe> pRecipeOutput, RecipeSerializer<?
            extends AbstractCookingRecipe> pSerializer, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult, float pExperience, int pCookingTime, String pGroup, String pSuffix) {
        for(ItemLike itemlike : pIngredients) {
            SimpleCookingRecipeBuilder.generic(Ingredient.of(itemlike), pCategory, pResult, pExperience, pCookingTime, pSerializer).group(pGroup).unlockedBy(getHasName(itemlike), has(itemlike))
                    .save(pRecipeOutput,
                            Ragnarok.MOD_ID + ":" + getItemName(pResult) + pSuffix + "_" + getItemName(itemlike));
        }
    }

    protected static void nineBlockStorageRecipes(Consumer<FinishedRecipe> pRecipeOutput,
                                                  RecipeCategory pUnpackedCategory,
                                                  ItemLike pUnpacked,
                                                  RecipeCategory pPackedCategory,
                                                  ItemLike pPacked) {
        ShapelessRecipeBuilder.shapeless(pUnpackedCategory, pUnpacked, 9)
                .requires(pPacked).unlockedBy(getHasName(pPacked), has(pPacked)).save(pRecipeOutput);
        ShapedRecipeBuilder.shaped(pPackedCategory, pPacked).define('#', pUnpacked)
                .pattern("###").pattern("###").pattern("###")
                .unlockedBy(getHasName(pUnpacked), has(pUnpacked)).save(pRecipeOutput);
    }
    protected static void nineBlockStorageRecipesRecipesWithCustomUnpacking(Consumer<FinishedRecipe> p_250320_, RecipeCategory p_248979_, ItemLike p_249101_, RecipeCategory p_252036_, ItemLike p_250886_, String p_248768_, String p_250847_) {
        nineBlockStorageRecipes(p_250320_, p_248979_, p_249101_, p_252036_, p_250886_, getSimpleRecipeName(p_250886_), (String)null, p_248768_, p_250847_);
    }

}
