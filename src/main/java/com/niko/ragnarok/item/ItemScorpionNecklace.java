package com.niko.ragnarok.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class ItemScorpionNecklace extends Item implements ICurioItem {
    public ItemScorpionNecklace() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        tooltipComponents.add(Component.translatable("tooltip.ragnarok.scorpion_necklace.description")
                .withStyle(net.minecraft.ChatFormatting.GREEN));
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
    }
}





