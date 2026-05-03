package com.niko.ragnarok.event;

import com.niko.ragnarok.entity.RagnarokEntities;
import com.niko.ragnarok.entity.costom.Groot;
import com.niko.ragnarok.entity.costom.Magic_Golem;
import com.niko.ragnarok.item.ItemScorpionNecklace;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
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
    public static void onAnimalKilled(LivingDeathEvent event) {
        LivingEntity killed = event.getEntity();

        // 殺されたのが動物で、殺したのがプレイヤーの場合
        if (killed instanceof Animal && event.getSource().getEntity() instanceof Player) {
            Player killer = (Player) event.getSource().getEntity();

            // 周囲16ブロック以内のGrootを探す
            AABB searchBox = new AABB(
                    killed.getX() - 16.0D, killed.getY() - 8.0D, killed.getZ() - 16.0D,
                    killed.getX() + 16.0D, killed.getY() + 8.0D, killed.getZ() + 16.0D
            );

            List<Groot> nearbyGroots = killed.level().getEntitiesOfClass(Groot.class, searchBox);

            // 近くのGrootを全て敵対化
            for (Groot groot : nearbyGroots) {
                if (!groot.isAngry()) {
                    groot.onNearbyAnimalKilled(killer);
                }
            }
        }
    }
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        Level level = event.getLevel();

        if (!level.isClientSide && entity instanceof Raider raider) {

            if (raider.getCurrentRaid() != null && raider instanceof Evoker) {

                if (level.random.nextFloat() < 1F) {
                    Magic_Golem golem = new Magic_Golem(RagnarokEntities.MAGIC_GOLEM.get(), level);
                    golem.moveTo(raider.getX(), raider.getY(), raider.getZ(), raider.getYRot(), 0.0F);

                    level.addFreshEntity(golem);
                }
            }
        }
    }
}