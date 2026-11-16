package com.niko.ragnarok.entity.geckolib_entity.model;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.VolcanicSerpentEntity.VolcanicSerpentBodyEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

/** Reusable body-segment view of the shared Volcanic Serpent geometry. */
public class VolcanicSerpentBodyModel extends GeoModel<VolcanicSerpentBodyEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "geo/volcanic_serpent.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "textures/entity/volcanic_serpent.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "animations/volcanic_serpent.animation.json");

    @Override public ResourceLocation getModelResource(VolcanicSerpentBodyEntity entity) { return MODEL; }
    @Override public ResourceLocation getTextureResource(VolcanicSerpentBodyEntity entity) { return TEXTURE; }
    @Override public ResourceLocation getAnimationResource(VolcanicSerpentBodyEntity entity) { return ANIMATION; }

    @Override
    public void setCustomAnimations(VolcanicSerpentBodyEntity entity, long instanceId, AnimationState<VolcanicSerpentBodyEntity> state) {
        super.setCustomAnimations(entity, instanceId, state);
        CoreGeoBone all = getAnimationProcessor().getBone("all");
        CoreGeoBone head = getAnimationProcessor().getBone("head");
        CoreGeoBone body = getAnimationProcessor().getBone("body");
        CoreGeoBone tail = getAnimationProcessor().getBone("tail");
        CoreGeoBone tailExtension = getAnimationProcessor().getBone("bone10");

        setVisible(head, false);
        setVisible(body, true);
        setVisible(tail, false);
        setVisible(tailExtension, false);
        // Unified body geometry occupies Z=23..107; center it on this segment entity.
        if (all != null) all.setPosZ(-65.0F);
        if (body != null) {
            body.setRotX(entity.getXRot() * ((float) Math.PI / 180.0F));
            body.setRotZ(entity.getSerpentRoll());
        }
    }

    private static void setVisible(CoreGeoBone bone, boolean visible) {
        if (bone == null) return;
        bone.setHidden(!visible);
        bone.setChildrenHidden(!visible);
    }
}
