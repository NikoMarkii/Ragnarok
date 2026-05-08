package com.niko.ragnarok.entity.renderer;

import com.niko.ragnarok.entity.Model.magic_golem_model;
import com.niko.ragnarok.entity.costom.Magic_Golem;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class MagicGolemRenderer extends MobRenderer<Magic_Golem, magic_golem_model<Magic_Golem>> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("ragnarok", "textures/entity/magic_golem.png");

    public MagicGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new magic_golem_model<>(context.bakeLayer(magic_golem_model.LAYER_LOCATION)), 0.7F);
    }

    @Override
    public ResourceLocation getTextureLocation(Magic_Golem entity) {
        return TEXTURE; // 定義したテクスチャを返す
    }
}
