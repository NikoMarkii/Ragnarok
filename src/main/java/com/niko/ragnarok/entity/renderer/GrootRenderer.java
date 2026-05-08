package com.niko.ragnarok.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.niko.ragnarok.entity.Model.groot_model;
import com.niko.ragnarok.entity.costom.Groot;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class GrootRenderer extends MobRenderer<Groot, groot_model<Groot>> {
    private static final ResourceLocation GROOT_LOCATION = ResourceLocation.fromNamespaceAndPath("ragnarok", "textures/entity/groot_texture.png");

    private static final ResourceLocation GROOT_ANGRY_LOCATION =
            ResourceLocation.fromNamespaceAndPath("ragnarok", "textures/entity/groot_rage_eye_layer.png");

    public GrootRenderer(EntityRendererProvider.Context context) {
        super(context, new groot_model<>(context.bakeLayer(groot_model.LAYER_LOCATION)), 1.2F);
    }

    protected void scale(Groot p_113974_, PoseStack p_113975_, float p_113976_) {
        p_113975_.scale(1F, 1F, 1F);
    }

    @Override
    public ResourceLocation getTextureLocation(Groot entity) {
        if (entity.isAngry()) {
            return GROOT_ANGRY_LOCATION;
        }
        return GROOT_LOCATION;
    }
    @Override
    protected void setupRotations(Groot entity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks) {
        if (entity.isActuallyDying()) {
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(170.0F - rotationYaw));
            return;
        }
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks);
    }
}