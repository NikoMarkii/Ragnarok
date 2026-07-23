package com.niko.ragnarok.entity.geckolib_entity.model;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.geckolib_entity.Costom.GhostEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class GhostModel extends GeoModel<GhostEntity> {

    @Override
    public ResourceLocation getModelResource(GhostEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(
                Ragnarok.MOD_ID, "geo/ghost.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GhostEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(
                Ragnarok.MOD_ID, "textures/entity/ghost.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GhostEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(
                Ragnarok.MOD_ID, "animations/ghost.animation.json");
    }
    @Override
    public void setCustomAnimations(GhostEntity ghost, long instanceId, AnimationState<GhostEntity> animationState) {
        super.setCustomAnimations(ghost, instanceId, animationState);

        CoreGeoBone head = getAnimationProcessor().getBone("Head");

        if (head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);

            if (ghost.isAlive()) {
                head.setRotX(entityData.headPitch() * ((float) Math.PI / 180F));
                head.setRotY(entityData.netHeadYaw() * ((float) Math.PI / 180F));
            }
        }
    }
}
