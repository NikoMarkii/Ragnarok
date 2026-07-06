package com.niko.ragnarok.entity.geckolib_entity.model;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.geckolib_entity.Costom.Cassowary;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class CassowaryModel extends GeoModel<Cassowary> {
    @Override
    public ResourceLocation getModelResource(Cassowary animatable) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "geo/cassowary.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Cassowary animatable) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "textures/entity/cassowary.png");
    }

    @Override
    public ResourceLocation getAnimationResource(Cassowary animatable) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "animations/cassowary.animation.json");
    }

    @Override
    public void setCustomAnimations(Cassowary animatable, long instanceId, AnimationState<Cassowary> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        CoreGeoBone head = getAnimationProcessor().getBone("head");
        if (head != null && animatable.isAlive() && !animatable.isAttacking()) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
            head.setRotX(entityData.headPitch() * ((float) Math.PI / 180F));
            head.setRotY(entityData.netHeadYaw() * ((float) Math.PI / 180F));
        }
    }
}
