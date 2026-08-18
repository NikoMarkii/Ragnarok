package com.niko.ragnarok.entity.geckolib_entity.model.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.DinocampusEntity;
import com.niko.ragnarok.entity.geckolib_entity.model.DinocampusModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DinocampusRenderer extends GeoEntityRenderer<DinocampusEntity> {
    public DinocampusRenderer(EntityRendererProvider.Context context) {
        super(context, new DinocampusModel());
        this.shadowRadius = 1.5F;
    }

    protected void scale(DinocampusEntity entity, PoseStack poseStack, float partialTickTime) {
        poseStack.scale(1F, 1F, 1F);
    }
}
