package com.niko.ragnarok.item.Armor;

import com.google.common.base.Supplier;
import com.niko.ragnarok.item.Ragnarok_mainItems;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.LazyLoadedValue;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

public enum RagnarokArmorMaterials implements ArmorMaterial {
    NAITOMEA_ARMOR("naitomea", 40, new int[]{5, 8, 10, 5}, 25,
            SoundEvents.ARMOR_EQUIP_NETHERITE, 3.0F, 0.1F,
            () -> Ingredient.of(Ragnarok_mainItems.NAITOMEA_INGOD.get())),
    WOODEN_ARMOR("wooden",
            5, // 耐久度倍率（皮が5、鉄が15程度なので、序盤用として設定）
            new int[]{1, 2, 3, 1}, // 防御力（足、脚、胴、頭）計7ポイント
            15, // エンチャント適性（木製ツールと同様に少し高め）
            SoundEvents.ARMOR_EQUIP_GENERIC, // 装備音
            0.0F, // タフネス
            0.0F, // ノックバック耐性
            // 全ての板材（Planksタグ）で修復可能にする
            () -> Ingredient.of(ItemTags.PLANKS));

    private static final int[] HEALTH_PER_SLOT = new int[]{13, 15, 16, 11};
    private final String name;
    private final int durabilityMultiplier;
    private final int[] slotProtections;
    private final int enchantmentValue;
    private final SoundEvent sound;
    private final float toughness;
    private final float knockbackResistance;
    private final LazyLoadedValue<Ingredient> repairIngredient;

    RagnarokArmorMaterials(String name, int durability, int[] protection, int enchant, SoundEvent sound, float toughness, float knockback, Supplier<Ingredient> repair) {
        this.name = name;
        this.durabilityMultiplier = durability;
        this.slotProtections = protection;
        this.enchantmentValue = enchant;
        this.sound = sound;
        this.toughness = toughness;
        this.knockbackResistance = knockback;
        this.repairIngredient = new LazyLoadedValue<>(repair);
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return HEALTH_PER_SLOT[type.getSlot().getIndex()] * this.durabilityMultiplier;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return this.slotProtections[type.getSlot().getIndex()];
    }

    @Override
    public int getEnchantmentValue() {
        return this.enchantmentValue;
    }

    @Override
    public SoundEvent getEquipSound() {
        return this.sound;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return this.repairIngredient.get();
    }

    @Override
    public String getName() {
        return "ragnarok:" + this.name;
    }

    @Override
    public float getToughness() {
        return this.toughness;
    }

    @Override
    public float getKnockbackResistance() {
        return this.knockbackResistance;
    }
}

