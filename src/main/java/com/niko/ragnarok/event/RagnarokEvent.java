package com.niko.ragnarok.event;

import com.niko.ragnarok.entity.costom.Groot;
import com.niko.ragnarok.item.ItemScorpionNecklace;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosCapability;

import java.util.List;

@Mod.EventBusSubscriber(modid = "ragnarok")
public class RagnarokEvent {

    @SubscribeEvent
    public static void onPlayerAttack(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {

            player.getCapability(CuriosCapability.INVENTORY).ifPresent(handler -> {

                handler.findFirstCurio(stack -> stack.getItem() instanceof ItemScorpionNecklace)
                        .ifPresent(slotResult -> {
                            LivingEntity target = event.getEntity();
                            target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
                        });
            });
        }
    }
    @SubscribeEvent
    public static void onAnimalDeath(LivingDeathEvent event) {
        // 死んだのが動物、かつ犯人がプレイヤーの場合
        if (event.getEntity() instanceof Animal && event.getSource().getEntity() instanceof Player player) {
            // 周囲のグルートを探して怒らせる
            List<Groot> groots = event.getEntity().level().getEntitiesOfClass(Groot.class,
                    event.getEntity().getBoundingBox().inflate(20.0D));
            for (Groot groot : groots) {
                groot.setTarget(player);
                groot.setAngry(true); // 前に作ったメソッドだね
            }
        }
    }
}