package com.niko.ragnarok.entity.geckolib_entity.model;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.VolcanicSerpentEntity.VolcanicSerpentHeadEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class VolcanicSerpentHeadModel extends GeoModel<VolcanicSerpentHeadEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "geo/volcanic_serpent_head.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "textures/entity/volcanic_serpent_head.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "animations/volcanic_serpent.animation.json");

    @Override public ResourceLocation getModelResource(VolcanicSerpentHeadEntity entity) { return MODEL; }
    @Override public ResourceLocation getTextureResource(VolcanicSerpentHeadEntity entity) { return TEXTURE; }
    @Override public ResourceLocation getAnimationResource(VolcanicSerpentHeadEntity entity) { return ANIMATION; }

    @Override
    public void setCustomAnimations(VolcanicSerpentHeadEntity entity, long instanceId, AnimationState<VolcanicSerpentHeadEntity> state) {
        super.setCustomAnimations(entity, instanceId, state);
        CoreGeoBone head = getAnimationProcessor().getBone("head");
        if (head != null) {
            head.setRotX(entity.getXRot() * ((float) Math.PI / 180.0F));
            head.setRotZ(entity.getSerpentRoll());
        }
    }
}
