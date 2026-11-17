
package com.niko.ragnarok.entity.geckolib_entity.model;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.Volcanic_Serpent.VolcanicSerpentEntity;

import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

public class VolcanicSerpentHeadModel
        extends GeoModel<VolcanicSerpentEntity> {

    @Override
    public ResourceLocation getModelResource(VolcanicSerpentEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "geo/volcanic_serpent.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(VolcanicSerpentEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "textures/entity/volcanic_serpent.png");
    }

    @Override
    public ResourceLocation getAnimationResource(VolcanicSerpentEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "animations/volcanic_serpent.animation.json");
    }

    @Override
    public void setCustomAnimations(
            VolcanicSerpentEntity entity,
            long instanceId,
            AnimationState<VolcanicSerpentEntity> animationState
    ) {
        super.setCustomAnimations(entity, instanceId, animationState);

        hideBone("head", false);
        hideBone("body", true);
        hideBone("tail", true);
        hideBone("tail2", true);
    }

    private void hideBone(String name, boolean hidden) {
        CoreGeoBone bone = getAnimationProcessor().getBone(name);

        if (bone != null) {
            bone.setHidden(hidden);
        }
    }
}
