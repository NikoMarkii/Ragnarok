package com.niko.ragnarok.entity.geckolib_entity.model.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.Volcanic_Serpent.VolcanicSerpentPartEntity;
import com.niko.ragnarok.entity.geckolib_entity.model.VolcanicSerpentPartModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class VolcanicSerpentPartRenderer
        extends GeoEntityRenderer<VolcanicSerpentPartEntity> {

    public VolcanicSerpentPartRenderer(EntityRendererProvider.Context context) {
        super(context, new VolcanicSerpentPartModel());
        this.shadowRadius = 1.5F;
    }

    @Override
    protected void applyRotations(
            VolcanicSerpentPartEntity animatable,
            PoseStack poseStack,
            float ageInTicks,
            float rotationYaw,
            float partialTick
    ) {
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick);
        float pitch = Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot());
        poseStack.translate(0.0D, 1.25D, 0.0D);
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.translate(0.0D, -1.25D, 0.0D);
    }

    @Override
    public boolean shouldShowName(VolcanicSerpentPartEntity entity) {
        return false;
    }
}