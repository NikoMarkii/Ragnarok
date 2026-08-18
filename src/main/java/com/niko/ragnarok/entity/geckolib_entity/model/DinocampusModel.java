package com.niko.ragnarok.entity.geckolib_entity.model;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.DinocampusEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class DinocampusModel extends GeoModel<DinocampusEntity> {
    @Override
    public ResourceLocation getModelResource(DinocampusEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "geo/dinocampus.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DinocampusEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "textures/entity/dinocampus.png");
    }

    @Override
    public ResourceLocation getAnimationResource(DinocampusEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "animations/dinocampus.animation.json");
    }

    @Override
    public void setCustomAnimations(DinocampusEntity animatable, long instanceId, AnimationState<DinocampusEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        CoreGeoBone head = getAnimationProcessor().getBone("head");

        if (head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);

            if (!animatable.isActuallyDying()) {
                // 67.5度（モデルの初期Rotation）をラジアンに変換して加算
                float defaultPitch = 42.5F * ((float) Math.PI / 180F);

                head.setRotX(defaultPitch + (entityData.headPitch() * ((float) Math.PI / 180F)));
                head.setRotY(entityData.netHeadYaw() * ((float) Math.PI / 180F));
            }
        }
    }
}
