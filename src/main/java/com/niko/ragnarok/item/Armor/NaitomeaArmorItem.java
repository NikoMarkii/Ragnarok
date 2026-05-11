package com.niko.ragnarok.item.Armor;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class NaitomeaArmorItem extends ArmorItem {
    public NaitomeaArmorItem(ArmorMaterial material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide() && entity instanceof Player player) {
            // このアイテムが「現在装備スロットにある」ときだけチェック
            // 4部位のうちどれか一つ（例えばヘルメット）で代表して判定する
            if (isEquipped(stack, player) && this.getType() == Type.HELMET) {
                if (hasFullSet(player)) {
                    applyFullSetBonus(player);
                }
            }
        }
    }

    private boolean isEquipped(ItemStack stack, Player player) {
        // 現在のアイテムスタックがプレイヤーのアーマースロットのいずれかと一致するか
        for (ItemStack armor : player.getArmorSlots()) {
            if (armor == stack) return true;
        }
        return false;
    }

    private boolean hasFullSet(Player player) {
        for (ItemStack armorStack : player.getArmorSlots()) {
            // 全部位がナイトメア防具（このクラスのインスタンス）であることを確認
            if (armorStack.isEmpty() || !(armorStack.getItem() instanceof NaitomeaArmorItem)) {
                return false;
            }
        }
        return true;
    }

    private void applyFullSetBonus(Player player) {
        // ニコ氏、ここが「終焉の使者」の力だ
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 1, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 40, 0, false, false, false));
    }
}
