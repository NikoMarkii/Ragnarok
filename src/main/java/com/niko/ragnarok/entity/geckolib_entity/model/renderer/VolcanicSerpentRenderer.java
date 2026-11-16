package com.niko.ragnarok.entity.geckolib_entity.model.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.VolcanicSerpent.VolcanicSerpentBodyEntity;
import com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.VolcanicSerpent.VolcanicSerpentHeadEntity;
import com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.VolcanicSerpent.VolcanicSerpentPartEntity;
import com.niko.ragnarok.entity.geckolib_entity.model.VolcanicSerpentModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class VolcanicSerpentRenderer extends GeoEntityRenderer<VolcanicSerpentHeadEntity> {

    public VolcanicSerpentRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new VolcanicSerpentModel());
    }

    @Override
    public boolean shouldRender(VolcanicSerpentHeadEntity head, Frustum camera, double camX, double camY, double camZ) {
        AABB expandedBox = head.getBoundingBox().inflate(64.0D);
        return camera.isVisible(expandedBox);
    }

    @Override
    public void render(VolcanicSerpentHeadEntity head, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        // 1. 頭部 (head) の描画
        setBoneVisibility("head", true);
        setBoneVisibility("body", false);
        setBoneVisibility("tail", false);

        poseStack.pushPose();
        float headPitch = Mth.lerp(partialTick, head.xRotO, head.getXRot());
        float headRoll = head.getRoll(partialTick);
        poseStack.mulPose(Axis.XP.rotationDegrees(headPitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(headRoll));
        super.render(head, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();

        // 2. 胴体 (body) と 尻尾 (tail) の描画
        double headX = Mth.lerp(partialTick, head.xo, head.getX());
        double headY = Mth.lerp(partialTick, head.yo, head.getY());
        double headZ = Mth.lerp(partialTick, head.zo, head.getZ());

        VolcanicSerpentBodyEntity body = head.getBody();
        VolcanicSerpentPartEntity[] parts = head.getParts();

        for (int i = 0; i < parts.length; i++) {
            VolcanicSerpentPartEntity part = parts[i];
            boolean isTail = part.isTail();

            setBoneVisibility("head", false);
            setBoneVisibility("body", !isTail);
            setBoneVisibility("tail", isTail);

            Vec3 partPos = body.getRenderPosition(i, partialTick);
            double relX = partPos.x - headX;
            double relY = partPos.y - headY;
            double relZ = partPos.z - headZ;

            float partYaw = body.getRenderYaw(i, partialTick);
            float partPitch = body.getRenderPitch(i, partialTick);
            float partRoll = body.getRenderRoll(i, partialTick);

            poseStack.pushPose();
            poseStack.translate(relX, relY, relZ);
            poseStack.mulPose(Axis.YP.rotationDegrees(-partYaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(partPitch));
            poseStack.mulPose(Axis.ZP.rotationDegrees(partRoll));

            super.render(head, 0.0F, partialTick, poseStack, bufferSource, packedLight);
            poseStack.popPose();
        }

        // 描画後にボーン表示状態を元に戻す
        setBoneVisibility("head", true);
        setBoneVisibility("body", true);
        setBoneVisibility("tail", true);
    }

    private void setBoneVisibility(String boneName, boolean visible) {
        this.model.getBone(boneName).ifPresent(bone -> bone.setHidden(!visible));
    }
}