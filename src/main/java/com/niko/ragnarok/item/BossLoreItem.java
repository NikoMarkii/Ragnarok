package com.niko.ragnarok.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BossLoreItem extends Item {
    private final int loreLineCount;

    public BossLoreItem(int loreLineCount, Properties properties) {
        super(properties);
        this.loreLineCount = loreLineCount;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.ragnarok.boss_lore.category").withStyle(ChatFormatting.DARK_PURPLE));
        if (Screen.hasShiftDown()) {
            for (int lineIndex = 1; lineIndex <= loreLineCount; lineIndex++) {
                tooltip.add(Component.translatable(getDescriptionId() + ".lore." + lineIndex)
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.ragnarok.boss_lore.hold_shift").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
