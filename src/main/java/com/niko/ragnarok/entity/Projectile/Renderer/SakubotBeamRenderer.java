package com.niko.ragnarok.entity.Projectile.Renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.Projectile.Model.sakubot_beam_model;
import com.niko.ragnarok.entity.Projectile.Model.slash_model;
import com.niko.ragnarok.entity.Projectile.SakubotBeamEntity;
import com.niko.ragnarok.entity.Projectile.VoidSlashEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class SakubotBeamRenderer extends EntityRenderer <SakubotBeamEntity>{
    private final sakubot_beam_model<SakubotBeamEntity> model;
    private static final ResourceLocation LAYER_LOCATION = ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "textures/entity/sakubot_beam.png");

    public SakubotBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new sakubot_beam_model<>(context.bakeLayer(sakubot_beam_model.LAYER_LOCATION));
    }
    @Override
    public void render(SakubotBeamEntity entity, float entityYaw, float particalTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(particalTicks, entity.yRotO, entity.getYRot()) - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(particalTicks, entity.xRotO, entity.getXRot())));

        int glowLight = LightTexture.FULL_BRIGHT;

        this.model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityTranslucentEmissive(getTextureLocation(entity))), glowLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F );
        poseStack.popPose();
        super.render(entity, entityYaw, particalTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SakubotBeamEntity p_114482_) {
        return LAYER_LOCATION;
    }
}
