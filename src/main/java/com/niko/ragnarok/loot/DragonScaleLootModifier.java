package com.niko.ragnarok.loot;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.niko.ragnarok.item.Ragnarok_mainItems;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class DragonScaleLootModifier extends LootModifier {
    public static final Supplier<Codec<DragonScaleLootModifier>> CODEC = Suppliers.memoize(()
            -> RecordCodecBuilder.create(inst -> codecStart(inst).and(ForgeRegistries.ITEMS.getCodec()
            .fieldOf("item").forGetter(m -> m.item)).apply(inst, DragonScaleLootModifier::new)));

    private final Item item;

    public DragonScaleLootModifier(LootItemCondition[] conditionsIn, Item item) {
        super(conditionsIn);
        this.item = item;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        for (LootItemCondition condition : this.conditions) {
            if (!condition.test(context)) {
                return generatedLoot;
            }
        }

        // 1. ドラゴンの鱗を64個追加
        generatedLoot.add(new ItemStack(Ragnarok_mainItems.DRAGON_SCALE.get(), 64));

        // 2. ナイトニウムの鍛冶型を1個追加
        // 登録名が NIGHTNIUM_UPGRADE_SMITHING_TEMPLATE であると仮定しているよ
        generatedLoot.add(new ItemStack(Ragnarok_mainItems.NIGHTNIUM_UPGRADE_SMITHING_TEMPLATE.get(), 1));

        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
