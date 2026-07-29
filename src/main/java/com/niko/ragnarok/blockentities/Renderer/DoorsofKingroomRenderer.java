package com.niko.ragnarok.blockentities.Renderer;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.niko.ragnarok.blockentities.DoorsofkingroomBlockEntity;
import com.niko.ragnarok.blockentities.Model.DoorsofKingroomModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class DoorsofKingroomRenderer extends GeoBlockRenderer<DoorsofkingroomBlockEntity> {

    public DoorsofKingroomRenderer(BlockEntityRendererProvider.Context context) {
        super(new DoorsofKingroomModel());
    }
    // ブロックのFACINGに合わせて描画回転を制御
    @Override
    public void rotateBlock(Direction facing, PoseStack poseStack) {
        switch (facing) {
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
            case WEST  -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
            case EAST  -> poseStack.mulPose(Axis.YP.rotationDegrees(270));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(0));
        }
    }
}
