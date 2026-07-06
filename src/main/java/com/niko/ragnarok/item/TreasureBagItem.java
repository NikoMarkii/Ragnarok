package com.niko.ragnarok.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TreasureBagItem extends Item {

    private final ResourceLocation lootTableId;

    public TreasureBagItem(ResourceLocation lootTableId, Properties properties) {
        super(properties);
        this.lootTableId = lootTableId;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand) {

        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }

        ServerLevel serverLevel = (ServerLevel) level;
        LootTable lootTable = serverLevel.getServer()
                .getLootData()
                .getLootTable(lootTableId);

        LootParams params = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .withParameter(LootContextParams.ORIGIN, player.position())
                .create(LootContextParamSets.GIFT);

        List<ItemStack> drops = lootTable.getRandomItems(params);

        for (ItemStack drop : drops) {
            if (!player.getInventory().add(drop)) {
                // インベントリが満杯なら足元にドロップ
                player.drop(drop, false);
            }
        }

        player.playSound(
                SoundEvents.BUNDLE_DROP_CONTENTS,
                1.0F,
                1.0F
        );

        stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag) {

        tooltip.add(Component.literal("右クリックで開く")
                .withStyle(ChatFormatting.GRAY));
    }
}