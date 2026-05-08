package com.niko.ragnarok.entity.geckolib_entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.niko.ragnarok.entity.geckolib_entity.Costom.Ender_Soldier;
import com.niko.ragnarok.entity.geckolib_entity.model.EnderSoldierModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class EnderSoldierRenderer extends GeoEntityRenderer<Ender_Soldier> {
    public EnderSoldierRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new EnderSoldierModel());

        addRenderLayer(new EnderSoldierEyesLayer(this));
    }
    @Override
    public void preRender(PoseStack poseStack, Ender_Soldier animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        float scale = 1.3f;
        poseStack.scale(scale, scale, scale);

        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public ResourceLocation getTextureLocation(Ender_Soldier animatable) {
        return ResourceLocation.fromNamespaceAndPath("ragnarok", "textures/entity/ender_soldier.png");
    }
    public class EnderSoldierEyesLayer extends GeoRenderLayer<Ender_Soldier> {
        private static final ResourceLocation GLOW_TEXTURE =
                ResourceLocation.fromNamespaceAndPath("ragnarok", "textures/entity/ender_soldier_glow.png");

        public EnderSoldierEyesLayer(GeoRenderer<Ender_Soldier> entityRendererIn) {
            super(entityRendererIn);
        }

        @Override
        public void render(PoseStack poseStack, Ender_Soldier animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

            RenderType eyesType = RenderType.eyes(GLOW_TEXTURE);

            this.getRenderer().actuallyRender(poseStack, animatable, bakedModel, eyesType, bufferSource,
                    bufferSource.getBuffer(eyesType), false, partialTick, 15728880,
                    OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
