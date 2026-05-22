package com.niko.ragnarok.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class FlowerNecklaceItem extends Item implements ICurioItem {
    public FlowerNecklaceItem(Properties properties) {
        super(properties);
    }

    // Curiosの機能：毎チック実行される処理
    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity entity = slotContext.entity();

        // サーバー側で、かつ40チック（2秒）に一度だけ判定（負荷軽減のため）
        if (!entity.level().isClientSide && entity.tickCount % 40 == 0) {
            // 常時「再生 I」を付与（時間は短くてOK、装備している限り更新される）
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, true, false, true));
        }
    }
}
