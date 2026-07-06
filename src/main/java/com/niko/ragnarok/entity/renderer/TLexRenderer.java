package com.niko.ragnarok.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.Model.t_lex_model;
import com.niko.ragnarok.entity.costom.Scorpion;
import com.niko.ragnarok.entity.costom.TLex;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class TLexRenderer extends MobRenderer<TLex, t_lex_model<TLex>> {

    private static final ResourceLocation TLEX_LOCATION =
            ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "textures/entity/tlex_texture.png");

    public TLexRenderer(EntityRendererProvider.Context pcontext) {
        super(pcontext, new t_lex_model<>(pcontext.bakeLayer(t_lex_model.LAYER_LOCATION)), 3f);
    }
    protected void scale(Scorpion p_113974_, PoseStack p_113975_, float p_113976_) {
        p_113975_.scale(1F, 1F, 1F);
    }
    @Override
    public ResourceLocation getTextureLocation(TLex p_114482_) {
        return TLEX_LOCATION;
    }
}

