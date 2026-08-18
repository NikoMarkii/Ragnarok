package com.niko.ragnarok.entity.Projectile.Renderer;

import com.niko.ragnarok.entity.Projectile.DinocampusBubbleEntity;
import com.niko.ragnarok.entity.geckolib_entity.model.DinocampusBubbleModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DinocampusBubbleRenderer extends GeoEntityRenderer<DinocampusBubbleEntity> {
    public DinocampusBubbleRenderer(EntityRendererProvider.Context context) {
        super(context, new DinocampusBubbleModel());
        this.shadowRadius = 0.2F;
    }
}
