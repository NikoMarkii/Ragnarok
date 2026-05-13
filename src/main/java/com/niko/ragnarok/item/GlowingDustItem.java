package com.niko.ragnarok.item;

import com.niko.ragnarok.entity.Projectile.GlowingDustEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class GlowingDustItem extends Item {
    public GlowingDustItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        // 投擲音（アレイの声などを使うと妖精らしいね）
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ALLAY_THROW, SoundSource.NEUTRAL, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!level.isClientSide) {
            // エンティティを生成して発射
            GlowingDustEntity dustEntity = new GlowingDustEntity(level, player);
            dustEntity.setItem(itemstack);
            dustEntity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            level.addFreshEntity(dustEntity);
        }

        // クリエイティブモードでなければ消費
        if (!player.getAbilities().instabuild) {
            itemstack.shrink(1);
        }

        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }
}
