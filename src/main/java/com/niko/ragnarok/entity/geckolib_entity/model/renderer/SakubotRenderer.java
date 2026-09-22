package com.niko.ragnarok.entity.geckolib_entity.model.renderer;

import com.niko.ragnarok.entity.geckolib_entity.Costom.SakuBotEntity;
import com.niko.ragnarok.entity.geckolib_entity.model.SakubotModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SakubotRenderer extends GeoEntityRenderer<SakuBotEntity> {
    public SakubotRenderer(EntityRendererProvider.Context context) {
        super(context, new SakubotModel());
        this.shadowRadius = 0.5F;
    }
}
