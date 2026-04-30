package com.niko.ragnarok.datagen.server;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.item.Ragnarok_mainItems;
import com.niko.ragnarok.loot.DragonScaleLootModifier;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.data.GlobalLootModifierProvider;
import net.minecraftforge.common.loot.LootTableIdCondition;

public class RagnarokGlobalLootModfierProvider extends GlobalLootModifierProvider {
    public RagnarokGlobalLootModfierProvider(PackOutput output) {
        super(output, Ragnarok.MOD_ID);
    }

    @Override
    protected void start() {
        add("dragon_scale_from_ender_dragon", new DragonScaleLootModifier(new LootItemCondition[]{
                new LootTableIdCondition.Builder(new ResourceLocation("entities/ender_dragon")).build()},
                Ragnarok_mainItems.DRAGON_SCALE.get()));
    }
}
