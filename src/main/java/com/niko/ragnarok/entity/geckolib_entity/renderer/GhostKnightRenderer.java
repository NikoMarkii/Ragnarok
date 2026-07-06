package com.niko.ragnarok.entity.geckolib_entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.niko.ragnarok.entity.geckolib_entity.Costom.GhostKnightEntity;
import com.niko.ragnarok.entity.geckolib_entity.model.GhostKnightModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;
import com.mojang.math.Axis;

public class GhostKnightRenderer extends GeoEntityRenderer<GhostKnightEntity> {

    public GhostKnightRenderer(EntityRendererProvider.Context context) {
        super(context, new GhostKnightModel());
        this.shadowRadius = 0.5F;

        addRenderLayer(new BlockAndItemGeoLayer<>(this) {

            @Override
            protected ItemStack getStackForBone(GeoBone bone, GhostKnightEntity animatable) {
                return switch (bone.getName()) {
                    case "right_hand_item" -> animatable.getMainHandItem();
                    default -> null;
                };
            }

            @Override
            protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, GhostKnightEntity animatable) {
                return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            }

            @Override
            protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack,
                                              GhostKnightEntity animatable, MultiBufferSource bufferSource,
                                              float partialTick, int packedLight, int packedOverlay) {
                poseStack.pushPose();

                poseStack.translate(0.0, 0.0, -0.05); // 位置調整（単位はブロック）

                poseStack.mulPose(Axis.XP.rotationDegrees(-90));   // X軸まわりに90度
                poseStack.mulPose(Axis.YP.rotationDegrees(0));   // Y軸まわりに45度
                poseStack.mulPose(Axis.ZP.rotationDegrees(0));    // Z軸まわりに0度

                poseStack.scale(1.2F, 1.2F, 1.2F); // 大きさ調整

                super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
                poseStack.popPose();
            }
        });
    }
}