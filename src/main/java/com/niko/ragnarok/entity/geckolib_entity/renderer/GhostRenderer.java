package com.niko.ragnarok.entity.geckolib_entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.geckolib_entity.Costom.GhostEntity;
import com.niko.ragnarok.entity.geckolib_entity.model.GhostModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class GhostRenderer extends GeoEntityRenderer<GhostEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    Ragnarok.MOD_ID, "textures/entity/ghost.png");

    public GhostRenderer(EntityRendererProvider.Context context) {
        super(context, new GhostModel());
        // 半透明＋発光レイヤーを追加
        this.addRenderLayer(new GhostGlowLayer(this));
    }

    // ── 半透明レンダリング ──
    // entityTranslucentCullで本体を半透明に
    @Override
    public RenderType getRenderType(
            GhostEntity entity,
            ResourceLocation texture,
            MultiBufferSource bufferSource,
            float partialTick) {
        return RenderType.entityTranslucentCull(texture);
    }

    // ── 発光レイヤー ──
    static class GhostGlowLayer extends GeoRenderLayer<GhostEntity> {

        private static final ResourceLocation GLOW_TEXTURE =
                ResourceLocation.fromNamespaceAndPath(
                        Ragnarok.MOD_ID, "textures/entity/ghost_glow.png");

        GhostGlowLayer(GeoEntityRenderer<GhostEntity> renderer) {
            super(renderer);
        }

        @Override
        public void render(
                PoseStack poseStack,
                GhostEntity entity,
                BakedGeoModel bakedModel,
                RenderType renderType,
                MultiBufferSource bufferSource,
                VertexConsumer buffer,
                float partialTick,
                int packedLight,
                int packedOverlay) {

            // eyesレンダータイプ = フルブライト＋加算合成で光って見える
            RenderType glowType = RenderType.eyes(GLOW_TEXTURE);
            VertexConsumer glowBuffer = bufferSource.getBuffer(glowType);

            getRenderer().reRender(
                    getDefaultBakedModel(entity),
                    poseStack,
                    bufferSource,
                    entity,
                    glowType,
                    glowBuffer,
                    partialTick,
                    packedLight,
                    packedOverlay,
                    1.0F, 1.0F, 1.0F, 1.0F
            );
        }
    }
}