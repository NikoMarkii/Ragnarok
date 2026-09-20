package com.niko.ragnarok.item.Armor;

import com.niko.ragnarok.item.Armor.Renderer.GradiusArmorRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * グラディウスアーマー（ヘルメット・チェストプレート・レギンス・ブーツ共通）。
 * GeckoLibの独自モデルで描画するため、GeoItemを実装している。
 */
public class GradiusArmorItem extends ArmorItem implements GeoItem {

    // イベント側からアクセスできるよう public で定義
    public static final UUID ATTACK_DAMAGE_BONUS_ID = UUID.fromString("c8d7e6f5-4321-1234-8765-abcdef123456");

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

    public static boolean isEquipped(ItemStack stack, Player player) {
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

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        if (level != null && level.isClientSide()) {
            Player player = Minecraft.getInstance().player;
            // 「プレイヤーがフルセット装備中」かつ「今マウスホバーしているこのスタック自体が装備枠にある」時だけ表示
            if (player != null && isEquipped(stack, player) && hasFullSet(player)) {
                tooltip.add(Component.translatable("tooltip.ragnarok.gradius_armor.fullset_title"));
                tooltip.add(Component.translatable("tooltip.ragnarok.gradius_armor.fullset_effect1"));
                tooltip.add(Component.translatable("tooltip.ragnarok.gradius_armor.fullset_effect2"));
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle", 0, state -> PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}