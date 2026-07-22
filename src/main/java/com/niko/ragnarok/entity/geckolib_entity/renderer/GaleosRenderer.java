package com.niko.ragnarok.entity.geckolib_entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.niko.ragnarok.entity.geckolib_entity.Costom.GaleosEntity;
import com.niko.ragnarok.entity.geckolib_entity.model.GaleosModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GaleosRenderer extends GeoEntityRenderer<GaleosEntity> {
    public GaleosRenderer(EntityRendererProvider.Context context) {
        super(context, new GaleosModel());
        this.shadowRadius = 1.5F;
    }
    protected void scale(GaleosEntity p_113974_, PoseStack p_113975_, float p_113976_) {
        p_113975_.scale(0.3F, 0.3F, 0.3F);
    }

}