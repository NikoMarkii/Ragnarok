package com.niko.ragnarok.entity.Projectile.Renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.Projectile.BlueFireballEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class BlueFireballRenderer extends EntityRenderer<BlueFireballEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    Ragnarok.MOD_ID,
                    "textures/entity/blue_fireball.png"
            );

    public BlueFireballRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(BlueFireballEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(
            BlueFireballEntity entity,
            float yaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {

        poseStack.pushPose();
        poseStack.scale(1F, 1F, 1F);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());

        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        int fullBright = 0xF000F0;

        // entityCutoutNoCull → 裏面カリングなし、確実に見える
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));

        // 1.20.1の正しい順序: vertex → color → uv → overlayCoords → uv2 → normal → endVertex
        vc.vertex(pose, -0.5F,  0.5F, 0.0F)
                .color(255, 255, 255, 255)
                .uv(0.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(fullBright)
                .normal(normal, 0.0F, 0.0F, 1.0F)
                .endVertex();

        vc.vertex(pose,  0.5F,  0.5F, 0.0F)
                .color(255, 255, 255, 255)
                .uv(1.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(fullBright)
                .normal(normal, 0.0F, 0.0F, 1.0F)
                .endVertex();

        vc.vertex(pose,  0.5F, -0.5F, 0.0F)
                .color(255, 255, 255, 255)
                .uv(1.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(fullBright)
                .normal(normal, 0.0F, 0.0F, 1.0F)
                .endVertex();

        vc.vertex(pose, -0.5F, -0.5F, 0.0F)
                .color(255, 255, 255, 255)
                .uv(0.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(fullBright)
                .normal(normal, 0.0F, 0.0F, 1.0F)
                .endVertex();

        poseStack.popPose();

        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    protected int getBlockLightLevel(BlueFireballEntity entity, BlockPos pos) {
        return 15;
    }
}
