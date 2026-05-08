package com.niko.ragnarok.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.niko.ragnarok.entity.costom.Mini_Groot;
import com.niko.ragnarok.entity.Model.mini_groot_model;
import com.niko.ragnarok.entity.costom.Scorpion;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class MiniGrootRenderer extends MobRenderer<Mini_Groot, mini_groot_model<Mini_Groot>> {
    public MiniGrootRenderer(EntityRendererProvider.Context context) {
        super(context, new mini_groot_model<>(context.bakeLayer(mini_groot_model.LAYER_LOCATION)), 0.5F);
    }
    @Override
    protected void scale(Mini_Groot entity, PoseStack poseStack, float partialTickTime) {
        poseStack.scale(0.5F, 0.5F, 0.5F);
    }
    @Override
    public ResourceLocation getTextureLocation(Mini_Groot entity) {
        return ResourceLocation.fromNamespaceAndPath("ragnarok", "textures/entity/mini_groot_texture.png");
    }
    @Override
    protected void setupRotations(Mini_Groot entity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks);

        if (entity.isInSittingPose()) {
            // 数値をプラス（正の数）にすることで、描画位置を上に持ち上げる
            // 画像の埋まり具合からすると 0.35D 〜 0.45D くらいが妥当かな
            poseStack.translate(0.0D, -0.2D, 0.0D);
        }
    }
}
