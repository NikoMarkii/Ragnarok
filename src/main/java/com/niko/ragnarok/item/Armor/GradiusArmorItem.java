package com.niko.ragnarok.item.Armor;

import com.niko.ragnarok.item.Armor.Renderer.GradiusArmorRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * グラディウスアーマー（ヘルメット・チェストプレート・レギンス・ブーツ共通）。
 * GeckoLibの独自モデルで描画するため、GeoItemを実装している。
 */
public class GradiusArmorItem extends ArmorItem implements GeoItem {

    private static final UUID ATTACK_DAMAGE_BONUS_ID = UUID.fromString("c8d7e6f5-4321-1234-8765-abcdef123456");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GradiusArmorItem(ArmorMaterial material, Type type, Properties properties) {
        super(material, type, properties);
    }

    // レンダラーの登録（1.19.3〜1.20.5系のForge向け）
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private GeoArmorRenderer<?> renderer;

            @Override
            public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack,
                                                                   EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (this.renderer == null) {
                    this.renderer = new GradiusArmorRenderer();
                }
                this.renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
                return this.renderer;
            }
        });
    }
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide() && entity instanceof Player player) {
            // ヘルメットのスロット処理でのみ代表してチェック（二重処理防止）
            if (this.getType() == Type.HELMET && isEquipped(stack, player)) {
                if (hasFullSet(player)) {
                    applyAttackBonus(player);
                } else {
                    removeAttackBonus(player);
                }
            }
        }
    }

    private boolean isEquipped(ItemStack stack, Player player) {
        for (ItemStack armor : player.getArmorSlots()) {
            if (armor == stack) return true;
        }
        return false;
    }

    public static boolean hasFullSet(Player player) {
        for (ItemStack armorStack : player.getArmorSlots()) {
            // 全部位がグラディウス防具（RagnarokArmorMaterials.GRADIUS_ARMOR）か確認
            if (armorStack.isEmpty() || !(armorStack.getItem() instanceof ArmorItem armorItem)) {
                return false;
            }
            if (armorItem.getMaterial() != RagnarokArmorMaterials.GRADIUS_ARMOR) {
                return false;
            }
        }
        return true;
    }

    private void applyAttackBonus(Player player) {
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr != null && attr.getModifier(ATTACK_DAMAGE_BONUS_ID) == null) {
            // 加算値（例: 近接攻撃力 +3.0D）。必要に応じて加算（ADDITION）か乗算（MULTIPLY_BASE）を選択
            AttributeModifier modifier = new AttributeModifier(
                    ATTACK_DAMAGE_BONUS_ID,
                    "Gradius Fullset Attack Bonus",
                    3.0D,
                    AttributeModifier.Operation.ADDITION
            );
            attr.addTransientModifier(modifier);
        }
    }

    private void removeAttackBonus(Player player) {
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr != null && attr.getModifier(ATTACK_DAMAGE_BONUS_ID) != null) {
            attr.removeModifier(ATTACK_DAMAGE_BONUS_ID);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 今は特にアニメーションしない（待機ポーズのみ）ので、常にSTOPで固定。
        // 発光演出などを付けたくなったら、ここに条件を追加する。
        controllers.add(new AnimationController<>(this, "idle", 0, state -> PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}