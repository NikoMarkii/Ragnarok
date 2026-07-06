package com.niko.ragnarok.entity.geckolib_entity.model;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.Gradius;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class GradiusModel extends GeoModel<Gradius> {

    @Override
    public ResourceLocation getModelResource(Gradius animatable) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID,
                "geo/gradius.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Gradius animatable) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID,
                "textures/entity/gradius_texture.png");
    }

    @Override
    public ResourceLocation getAnimationResource(Gradius animatable) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID,
                "animations/gradius.animation.json");
    }

    @Override
    public void setCustomAnimations(
            Gradius animatable,
            long instanceId,
            AnimationState<Gradius> animationState) {

        super.setCustomAnimations(animatable, instanceId, animationState);

        CoreGeoBone head = getAnimationProcessor().getBone("head");
        CoreGeoBone armor = getAnimationProcessor().getBone("armor1");
        CoreGeoBone helmet = getAnimationProcessor().getBone("helmet");

        // 首振り
        if (head != null) {
            EntityModelData entityData =
                    animationState.getData(DataTickets.ENTITY_MODEL_DATA);

            if (!animatable.isActuallyDying() && !animatable.isStandby() && !animatable.isStandbyEnding()) {
                head.setRotX(entityData.headPitch() * ((float)Math.PI / 180F));
                head.setRotY(entityData.netHeadYaw() * ((float)Math.PI / 180F));
            }
        }

        // 覚醒演出で非表示
        boolean hideArmor =
                animatable.isPhase2()
                        || animatable.getAwakeningTimer() >= 25;

        if (armor != null) {
            armor.setHidden(hideArmor);
        }

        if (helmet != null) {
            helmet.setHidden(hideArmor);
        }
    }
}
