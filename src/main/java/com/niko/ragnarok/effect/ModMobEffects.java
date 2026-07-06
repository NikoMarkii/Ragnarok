package com.niko.ragnarok.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


public class ModMobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, "ragnarok"); // MOD IDを入れてね

    // この VOID_REAPER_EFFECT が、アイテム側で呼ぶ時の名前になる
    public static final RegistryObject<MobEffect> VOID_REAPER_EFFECT = MOB_EFFECTS.register("void_reaper",
            () -> new VoidReaperEffect(MobEffectCategory.HARMFUL, 0x3b005d)); // 禍々しい紫色
}
