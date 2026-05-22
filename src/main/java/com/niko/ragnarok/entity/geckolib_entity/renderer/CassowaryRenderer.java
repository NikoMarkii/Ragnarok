package com.niko.ragnarok.entity.geckolib_entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.niko.ragnarok.entity.geckolib_entity.Costom.Cassowary;
import com.niko.ragnarok.entity.geckolib_entity.model.CassowaryModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CassowaryRenderer extends GeoEntityRenderer<Cassowary> {
    public CassowaryRenderer(EntityRendererProvider.Context context) {
        super(context, new CassowaryModel());
        this.shadowRadius = 0.7F;
    }

    @Override
    public void render(Cassowary entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(0.9F, 0.9F, 0.9F);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
