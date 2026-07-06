package com.niko.ragnarok.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.costom.RedCreeper;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class RedCreeperRenderer extends MobRenderer<RedCreeper,CreeperModel<RedCreeper>> {
    private static final ResourceLocation RED_CREEPER_LOCATION =
            ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID,"textures/entity/redcreeper.png");
    public RedCreeperRenderer(EntityRendererProvider.Context pcontext) {
        super(pcontext,
                new CreeperModel <>(pcontext.bakeLayer(ModelLayers.CREEPER)), 0.5f);
    }
    protected void scale(RedCreeper p_114046_, PoseStack p_114047_, float p_114048_) {
        float f = p_114046_.getSwelling(p_114048_);
        float f1 = 1.0F + Mth.sin(f * 100.0F) * f * 0.01F;
        f = Mth.clamp(f, 0.0F, 1.0F);
        f *= f;
        f *= f;
        float f2 = (1.0F + f * 0.4F) * f1;
        float f3 = (1.0F + f * 0.1F) / f1;
        p_114047_.scale(f2, f3, f2);
    }
    @Override
    public ResourceLocation getTextureLocation(RedCreeper p_114482_) {
        return RED_CREEPER_LOCATION;
    }
}
