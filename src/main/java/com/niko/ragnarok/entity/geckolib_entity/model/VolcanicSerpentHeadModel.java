
package com.niko.ragnarok.entity.geckolib_entity.model;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.Volcanic_Serpent.VolcanicSerpentEntity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.data.EntityModelData;

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

        CoreGeoBone head = getAnimationProcessor().getBone("head");
        if (head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
            head.setRotY(Mth.clamp(entityData.netHeadYaw(), -45.0F, 45.0F) * Mth.DEG_TO_RAD);
            head.setRotX(Mth.clamp(entityData.headPitch(), -70.0F, 70.0F) * Mth.DEG_TO_RAD);
        }
    }

    private void hideBone(String name, boolean hidden) {
        CoreGeoBone bone = getAnimationProcessor().getBone(name);

        if (bone != null) {
            bone.setHidden(hidden);
        }
    }
}
