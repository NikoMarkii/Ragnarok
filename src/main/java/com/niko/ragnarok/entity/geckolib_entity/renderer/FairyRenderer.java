package com.niko.ragnarok.entity.geckolib_entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.niko.ragnarok.entity.geckolib_entity.Costom.Fairy;
import com.niko.ragnarok.entity.geckolib_entity.model.FairyModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
 
/**
 * フェアリーのレンダラー
 */
public class FairyRenderer extends GeoEntityRenderer<Fairy> {
 
    public FairyRenderer(EntityRendererProvider.Context context) {
        super(context, new FairyModel());
        
        // フェアリーは小さいので0.6倍にスケール
        this.shadowRadius = 0.3F;
    }
 
    @Override
    public void render(Fairy entity, float entityYaw, float partialTick, 
                      PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        
        // 子供サイズの場合はさらに小さく
        if (entity.isBaby()) {
            poseStack.pushPose();
            poseStack.scale(0.5F, 0.5F, 0.5F);
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            poseStack.popPose();
        } else {
            poseStack.pushPose();
            poseStack.scale(0.6F, 0.6F, 0.6F);
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            poseStack.popPose();
        }
    }
}