package com.niko.ragnarok.entity.Projectile.Renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.Projectile.Model.slash_model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import com.niko.ragnarok.entity.Projectile.VoidSlashEntity;

public class VoidSlashRenderer extends EntityRenderer<VoidSlashEntity> {
    private final slash_model<VoidSlashEntity> model;
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "textures/entity/voidslash.png");

    public VoidSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
        // モデルの初期化（LayerDefinitionの登録が必要）
        this.model = new slash_model<>(context.bakeLayer(slash_model.LAYER_LOCATION));
    }

    // VoidSlashRenderer.java の render メソッド
    @Override
    public void render(VoidSlashEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // 回転は水平に固定
        poseStack.mulPose(Axis.YP.rotationDegrees(-180F - entity.getViewYRot(partialTicks)));
        // XP（縦回転）を入れないことで、常に水平を保つ

        // 【重要】余計な translate はすべて消去！
        // これで当たり判定の中心にモデルが来る

        poseStack.scale(4.0F, 4.0F, 4.0F);

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.eyes(TEXTURE));
        this.model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
    @Override
    public ResourceLocation getTextureLocation(VoidSlashEntity entity) {
        return TEXTURE;
    }
}
