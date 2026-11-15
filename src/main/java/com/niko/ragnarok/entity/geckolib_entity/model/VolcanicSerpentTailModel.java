package com.niko.ragnarok.entity.geckolib_entity.model;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.VolcanicSerpentEntity.VolcanicSerpentTailEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class VolcanicSerpentTailModel extends GeoModel<VolcanicSerpentTailEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "geo/volcanic_serpent_tail.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "textures/entity/volcanic_serpent_tail.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "animations/volcanic_serpent.animation.json");

    @Override public ResourceLocation getModelResource(VolcanicSerpentTailEntity entity) { return MODEL; }
    @Override public ResourceLocation getTextureResource(VolcanicSerpentTailEntity entity) { return TEXTURE; }
    @Override public ResourceLocation getAnimationResource(VolcanicSerpentTailEntity entity) { return ANIMATION; }

    @Override
    public void setCustomAnimations(VolcanicSerpentTailEntity entity, long instanceId, AnimationState<VolcanicSerpentTailEntity> state) {
        super.setCustomAnimations(entity, instanceId, state);
        CoreGeoBone tail = getAnimationProcessor().getBone("tail");
        if (tail != null) {
            tail.setRotX(entity.getXRot() * ((float) Math.PI / 180.0F));
            tail.setRotZ(entity.getSerpentRoll());
        }
    }
}
