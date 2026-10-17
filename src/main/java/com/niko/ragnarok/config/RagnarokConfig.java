package com.niko.ragnarok.config;

import com.niko.ragnarok.Ragnarok;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Map;

public final class RagnarokConfig {
    public static final String FILE_NAME = "ragnarok-common.toml";
    public static final ForgeConfigSpec SPEC;
    private static final Map<ResourceLocation, Stats> BOSSES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Boss base stats. Restart the game/server after editing.",
                "Health and damage use points (2 points = 1 heart).",
                "Existing bosses keep their remaining health percentage when loaded.").push("bosses");
        BOSSES = Map.of(
                id("gradius"), new Stats(builder, "gradius", 500, 18, 12, 21),
                id("dinocampus"), new Stats(builder, "dinocampus", 450, 28, 15, 21),
                id("galeos"), new Stats(builder, "galeos", 200, 10, 10, 0),
                id("ender_soldier"), new Stats(builder, "ender_soldier", 200, 12, 15, 0));
        builder.pop();
        SPEC = builder.build();
    }

    private RagnarokConfig() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, path);
    }

    @Nullable
    public static Stats forEntity(LivingEntity entity) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return id == null ? null : BOSSES.get(id);
    }

    public static float capDamage(LivingEntity boss, DamageSource source, float amount, float fallback) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return amount;
        Stats stats = forEntity(boss);
        double cap = stats == null ? fallback : stats.damageCap.get();
        return cap > 0.0D ? (float) Math.min(amount, cap) : amount;
    }

    // Fixed-damage projectiles follow the configured base attack, without scaling melee twice.
    public static float scaleSpecialDamage(@Nullable LivingEntity owner, float damage) {
        Stats stats = owner == null ? null : forEntity(owner);
        return stats == null ? damage
                : (float) (damage * owner.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) / stats.defaultAttackDamage);
    }

    public static final class Stats {
        public final ForgeConfigSpec.DoubleValue health;
        public final ForgeConfigSpec.DoubleValue attackDamage;
        public final ForgeConfigSpec.DoubleValue armor;
        public final ForgeConfigSpec.DoubleValue damageCap;
        private final double defaultAttackDamage;

        private Stats(ForgeConfigSpec.Builder builder, String name, double health, double attack,
                      double armor, double cap) {
            this.defaultAttackDamage = attack;
            builder.push(name);
            this.health = builder.comment("Base maximum health.").worldRestart()
                    .defineInRange("health", health, 1.0D, 1024.0D);
            this.attackDamage = builder.comment("Base attack damage. Attack multipliers still apply.").worldRestart()
                    .defineInRange("attack_damage", attack, 0.0D, 2048.0D);
            this.armor = builder.comment("Base armor points.").worldRestart()
                    .defineInRange("armor", armor, 0.0D, 30.0D);
            this.damageCap = builder.comment("Maximum incoming damage per hit BEFORE armor and boss reductions.",
                            "0 disables the cap. /kill and other invulnerability-bypassing damage are exempt.")
                    .worldRestart().defineInRange("damage_cap", cap, 0.0D, 1000000.0D);
            builder.pop();
        }

        public void apply(LivingEntity entity) {
            var maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
            var attack = entity.getAttribute(Attributes.ATTACK_DAMAGE);
            var defense = entity.getAttribute(Attributes.ARMOR);
            if (maxHealth == null || attack == null || defense == null) return;

            float oldMax = entity.getMaxHealth();
            float oldHealth = entity.getHealth();
            maxHealth.setBaseValue(this.health.get());
            attack.setBaseValue(this.attackDamage.get());
            defense.setBaseValue(this.armor.get());
            // Only change base values: phase buffs and other attribute modifiers must survive.
            if (oldMax != entity.getMaxHealth()) {
                entity.setHealth(oldHealth / oldMax * entity.getMaxHealth());
            }
        }
    }
}
