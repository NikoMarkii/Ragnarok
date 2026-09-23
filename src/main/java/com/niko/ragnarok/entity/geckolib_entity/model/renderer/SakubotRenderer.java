package com.niko.ragnarok.entity.geckolib_entity.model.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.niko.ragnarok.entity.geckolib_entity.Costom.SakuBotEntity;
import com.niko.ragnarok.entity.geckolib_entity.model.SakubotModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class SakubotRenderer extends GeoEntityRenderer<SakuBotEntity> {
    public SakubotRenderer(EntityRendererProvider.Context context) {
        super(context, new SakubotModel());
        this.shadowRadius = 0.5F;
        addRenderLayer(new SakubotGlowingLayer(this));
    }
    public class SakubotGlowingLayer extends GeoRenderLayer<SakuBotEntity> {
        private static final ResourceLocation GLOW_TEXTURE =
                ResourceLocation.fromNamespaceAndPath("ragnarok", "textures/entity/sakubot_grow.png");

        public SakubotGlowingLayer(GeoRenderer<SakuBotEntity> entityRendererIn) {
            super(entityRendererIn);
        }
        @Override
        public void render(PoseStack poseStack, SakuBotEntity animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay){
            RenderType eyesType = RenderType.eyes(GLOW_TEXTURE);
            this.getRenderer().actuallyRender(poseStack, animatable, bakedModel, eyesType, bufferSource,
                    bufferSource.getBuffer(eyesType), false, partialTick, 15728880,
                    OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        }
        }
    }
