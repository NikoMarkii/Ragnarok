package com.niko.ragnarok.entity.geckolib_entity.model;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.geckolib_entity.Costom.GhostKnightEntity;
import com.niko.ragnarok.entity.geckolib_entity.Costom.GhostWizardEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class GhostWizardModel extends GeoModel <GhostWizardEntity> {

    @Override
    public ResourceLocation getModelResource(GhostWizardEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID,
                "geo/ghost_wizard.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GhostWizardEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID,
                "textures/entity/ghost_wizard.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GhostWizardEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID,
                "animations/ghost_wizard.animation.json");
    }
    @Override
    public void setCustomAnimations(GhostWizardEntity ghostWizardEntity, long instanceId, AnimationState<GhostWizardEntity> animationState) {
        super.setCustomAnimations(ghostWizardEntity, instanceId, animationState);

        CoreGeoBone head = getAnimationProcessor().getBone("head");

        if (head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);

            if (ghostWizardEntity.isAlive()) {
                head.setRotX(entityData.headPitch() * ((float) Math.PI / 180F));
                head.setRotY(entityData.netHeadYaw() * ((float) Math.PI / 180F));
            }
        }
    }
}
