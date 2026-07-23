package com.niko.ragnarok.entity.geckolib_entity.model;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.geckolib_entity.Costom.GaleosEntity;
import com.niko.ragnarok.entity.geckolib_entity.Costom.GhostKnightEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class GaleosModel extends GeoModel<GaleosEntity> {

    @Override
    public ResourceLocation getModelResource(GaleosEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "geo/galeos.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GaleosEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "textures/entity/galeos.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GaleosEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "animations/galeos.animation.json");
    }
    @Override
    public void setCustomAnimations(GaleosEntity galeosEntity, long instanceId, AnimationState<GaleosEntity> animationState) {
        super.setCustomAnimations(galeosEntity, instanceId, animationState);

        CoreGeoBone head = getAnimationProcessor().getBone("head");

        if (head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);

            if (galeosEntity.isAlive()) {
                head.setRotX(entityData.headPitch() * ((float) Math.PI / 180F));
                head.setRotY(entityData.netHeadYaw() * ((float) Math.PI / 180F));
            }
        }
    }
}