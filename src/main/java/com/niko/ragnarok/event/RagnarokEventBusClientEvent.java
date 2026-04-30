package com.niko.ragnarok.event;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.Model.groot_model;
import com.niko.ragnarok.entity.RagnarokEntities;
import com.niko.ragnarok.entity.Model.scorpion_model;
import com.niko.ragnarok.entity.Model.t_lex_model;
import com.niko.ragnarok.entity.renderer.GrootRenderer;
import com.niko.ragnarok.entity.renderer.RedCreeperRenderer;
import com.niko.ragnarok.entity.renderer.ScorpionRenderer;
import com.niko.ragnarok.entity.renderer.TLexRenderer;
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
    }
}
