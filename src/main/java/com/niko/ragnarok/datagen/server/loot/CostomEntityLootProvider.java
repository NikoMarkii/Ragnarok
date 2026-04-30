package com.niko.ragnarok.datagen.server.loot;

import com.niko.ragnarok.entity.RagnarokEntities;
import com.niko.ragnarok.item.Ragnarok_mainItems;
import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.LootingEnchantFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraftforge.registries.RegistryObject;

import java.util.stream.Stream;

import static net.minecraft.world.level.storage.loot.LootPool.lootPool;

public class CostomEntityLootProvider extends EntityLootSubProvider {
    protected CostomEntityLootProvider() {
        super(FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    public void generate() {
        this.add(RagnarokEntities.RED_CREEPER.get(), LootTable.lootTable().withPool(
                lootPool().setRolls(ConstantValue.exactly(1.0f))
                        .add(LootItem.lootTableItem(Items.FIRE_CHARGE))
                        .add(LootItem.lootTableItem(Items.GUNPOWDER))
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f,3.0f)))
                        .apply(LootingEnchantFunction.lootingMultiplier
                                (UniformGenerator.between(0.0f, 1.0f)))
                        ));
        this.add(RagnarokEntities.SCORPION.get(), LootTable.lootTable().withPool(
                lootPool().setRolls(ConstantValue.exactly(1.0f))
                        .add(LootItem.lootTableItem(Ragnarok_mainItems.SCORPION_CELL.get()))
                        .add(LootItem.lootTableItem(Ragnarok_mainItems.SCORPION_NEEDLE.get()))
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f,3.0f)))
                        .apply(LootingEnchantFunction.lootingMultiplier
                                (UniformGenerator.between(0.0f, 1.0f)))
        ));
        this.add(RagnarokEntities.T_LEX.get(), LootTable.lootTable().withPool(
                lootPool().setRolls(ConstantValue.exactly(1.0f))
                        .add(LootItem.lootTableItem(Items.BONE))
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f,3.0f)))
                        .apply(LootingEnchantFunction.lootingMultiplier
                                (UniformGenerator.between(0.0f, 1.0f)))
        ));
    }

    @Override
    protected Stream<EntityType<?>> getKnownEntityTypes() {
        return RagnarokEntities.ENTITY_TYPES.getEntries()
                .stream().map(RegistryObject::get);
    }
}