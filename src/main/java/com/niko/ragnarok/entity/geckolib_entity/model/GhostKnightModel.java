package com.niko.ragnarok.entity.geckolib_entity.model;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.geckolib_entity.Costom.GhostKnightEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class GhostKnightModel extends GeoModel<GhostKnightEntity> {

    @Override
    public ResourceLocation getModelResource(GhostKnightEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(
                Ragnarok.MOD_ID, "geo/ghost_knight.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GhostKnightEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(
                Ragnarok.MOD_ID, "textures/entity/ghost_knight.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GhostKnightEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(
                Ragnarok.MOD_ID, "animations/ghost_knight.animation.json");
    }
    @Override
    public void setCustomAnimations(GhostKnightEntity ghostKnight, long instanceId, AnimationState<GhostKnightEntity> animationState) {
        super.setCustomAnimations(ghostKnight, instanceId, animationState);

        CoreGeoBone head = getAnimationProcessor().getBone("head");

        if (head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);

            if (ghostKnight.isAlive()) {
                head.setRotX(entityData.headPitch() * ((float) Math.PI / 180F));
                head.setRotY(entityData.netHeadYaw() * ((float) Math.PI / 180F));
            }
        }
    }
}