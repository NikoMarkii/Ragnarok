package com.niko.ragnarok.item;

import com.niko.ragnarok.entity.RagnarokEntities;
import com.niko.ragnarok.entity.costom.Mini_Groot;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.ClipContext;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemGrootHeart extends Item {

    public ItemGrootHeart() {
        super(new Properties()
                .rarity(Rarity.RARE)
        );
    }
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        // サーバー側でのみエンティティを生成する
        if (!level.isClientSide) {
            // プレイヤーの視線の先のブロックを探す
            BlockHitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);

            // ミニグルートをスポーンさせる
            Mini_Groot miniGroot = RagnarokEntities.MINI_GROOT.get().spawn(
                    (net.minecraft.server.level.ServerLevel) level,
                    itemstack,
                    player,
                    hitResult.getBlockPos().above(),
                    MobSpawnType.SPAWN_EGG,
                    true,
                    true
            );

            if (miniGroot != null) {
                // 召喚に成功したらアイテムを1つ減らす
                if (!player.getAbilities().instabuild) {
                    itemstack.shrink(1);
                }
                player.awardStat(Stats.ITEM_USED.get(this));
                return InteractionResultHolder.consume(itemstack);
            }
        }

        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        // ツールチップの設定
        tooltip.add(Component.translatable("item.ragnarok.groot_heart.tooltip.1")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.ragnarok.groot_heart.tooltip.2")
                .withStyle(net.minecraft.ChatFormatting.DARK_RED));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
