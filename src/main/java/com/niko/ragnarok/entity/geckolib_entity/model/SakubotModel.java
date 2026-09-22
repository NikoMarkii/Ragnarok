package com.niko.ragnarok.entity.geckolib_entity.model;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.geckolib_entity.Costom.GhostWizardEntity;
import com.niko.ragnarok.entity.geckolib_entity.Costom.SakuBotEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class SakubotModel extends GeoModel<SakuBotEntity> {
    @Override
    public ResourceLocation getModelResource(SakuBotEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                Ragnarok.MOD_ID, "geo/sakubot.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SakuBotEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID,
                "textures/entity/sakubot.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SakuBotEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                Ragnarok.MOD_ID, "animations/sakubot.animation.json");
    }
    @Override
    public void setCustomAnimations(SakuBotEntity sakuBotEntity, long instanceId, AnimationState<SakuBotEntity> animationState) {
        super.setCustomAnimations(sakuBotEntity, instanceId, animationState);

        CoreGeoBone head = getAnimationProcessor().getBone("head");

        if (head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);

            if (sakuBotEntity.isAlive()) {
                head.setRotX(entityData.headPitch() * ((float) Math.PI / 180F));
                head.setRotY(entityData.netHeadYaw() * ((float) Math.PI / 180F));
            }
        }
    }
}
