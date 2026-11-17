package com.niko.ragnarok.entity.geckolib_entity.model;

import com.niko.ragnarok.Ragnarok;

import com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.Volcanic_Serpent.VolcanicSerpentPartEntity;
import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.core.animation.AnimationState;

public class VolcanicSerpentPartModel
        extends GeoModel<VolcanicSerpentPartEntity> {

    @Override
    public ResourceLocation getModelResource(VolcanicSerpentPartEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "geo/volcanic_serpent.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(VolcanicSerpentPartEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "textures/entity/volcanic_serpent.png");
    }

    @Override
    public ResourceLocation getAnimationResource(VolcanicSerpentPartEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "animations/volcanic_serpent.animation.json");
    }

    @Override
    public void setCustomAnimations(
            VolcanicSerpentPartEntity entity,
            long instanceId,
            AnimationState<VolcanicSerpentPartEntity> animationState
    ) {
        super.setCustomAnimations(entity, instanceId, animationState);

        boolean tail = entity.isTail();

        hideBone("head", true);
        hideBone("body", tail);
        hideBone("tail", !tail);
        hideBone("tail2", !tail);

        if (tail) {
            // 尻尾の中心をエンティティ原点に合わせる
            setBoneZ("tail", -183.0F);
            setBoneZ("tail2", -183.0F);
        } else {
            // 胴体の中心をエンティティ原点に合わせる
            setBoneZ("body", -65.0F);
        }
    }

    private void setBoneZ(String name, float z) {
        CoreGeoBone bone = getAnimationProcessor().getBone(name);

        if (bone != null) {
            bone.setPosZ(z);
        }
    }

    private void hideBone(String name, boolean hidden) {
        CoreGeoBone bone = getAnimationProcessor().getBone(name);

        if (bone != null) {
            bone.setHidden(hidden);
        }
    }
}