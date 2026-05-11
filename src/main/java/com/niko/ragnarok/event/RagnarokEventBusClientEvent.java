package com.niko.ragnarok.event;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.Model.groot_model;
import com.niko.ragnarok.entity.Model.mini_groot_model;
import com.niko.ragnarok.entity.Projectile.Model.slash_model;
import com.niko.ragnarok.entity.RagnarokEntities;
import com.niko.ragnarok.entity.Model.scorpion_model;
import com.niko.ragnarok.entity.Model.t_lex_model;
import com.niko.ragnarok.entity.Model.magic_golem_model;
import com.niko.ragnarok.entity.geckolib_entity.renderer.EnderSoldierRenderer;
import com.niko.ragnarok.entity.Projectile.Renderer.VoidSlashRenderer;
import com.niko.ragnarok.entity.renderer.*;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Ragnarok.MOD_ID,bus = Mod.EventBusSubscriber.Bus.MOD,value = Dist.CLIENT)
public class RagnarokEventBusClientEvent {
    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RagnarokEntities.RED_CREEPER.get(), RedCreeperRenderer::new);
        event.registerEntityRenderer(RagnarokEntities.SCORPION.get(), ScorpionRenderer::new);
        event.registerEntityRenderer(RagnarokEntities.T_LEX.get(), TLexRenderer::new);
        event.registerEntityRenderer(RagnarokEntities.GROOT.get(), GrootRenderer::new);
        event.registerEntityRenderer(RagnarokEntities.MINI_GROOT.get(), MiniGrootRenderer::new);
        event.registerEntityRenderer(RagnarokEntities.MAGIC_GOLEM.get(),MagicGolemRenderer::new);
        event.registerEntityRenderer(RagnarokEntities.ENDER_SOLDIER.get(), EnderSoldierRenderer::new);
        EntityRenderers.register(RagnarokEntities.VOID_SLASH.get(), VoidSlashRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(
            EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(scorpion_model.SCORPION_LAYER_LOCATION,
                scorpion_model::createBodyLayer);
        event.registerLayerDefinition(t_lex_model.LAYER_LOCATION,
                t_lex_model::createBodyLayer);
        event.registerLayerDefinition(groot_model.LAYER_LOCATION,
                groot_model::createBodyLayer);
        event.registerLayerDefinition(mini_groot_model.LAYER_LOCATION,
                mini_groot_model::createBodyLayer);
        event.registerLayerDefinition(magic_golem_model.LAYER_LOCATION,
                magic_golem_model::createBodyLayer);
        event.registerLayerDefinition(slash_model.LAYER_LOCATION,
                slash_model::createBodyLayer);
    }
}
