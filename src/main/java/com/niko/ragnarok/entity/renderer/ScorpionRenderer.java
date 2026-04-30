package com.niko.ragnarok.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.Model.scorpion_model;
import com.niko.ragnarok.entity.costom.Scorpion;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class  ScorpionRenderer extends MobRenderer<Scorpion, scorpion_model<Scorpion>> {
    private static final ResourceLocation SCORPION_LOCATION =
            new ResourceLocation(Ragnarok.MOD_ID, "textures/entity/scorpion.png");

    public ScorpionRenderer(EntityRendererProvider.Context pcontext) {
        super(pcontext, new scorpion_model<>(pcontext.bakeLayer(scorpion_model.SCORPION_LAYER_LOCATION)), 0.8f);
        }
    protected void scale(Scorpion p_113974_, PoseStack p_113975_, float p_113976_) {
        p_113975_.scale(0.5F, 0.5F, 0.5F);
    }
    @Override
    public ResourceLocation getTextureLocation(Scorpion p_114482_) {
        return SCORPION_LOCATION;
    }
}
