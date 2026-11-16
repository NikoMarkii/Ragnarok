package com.niko.ragnarok.entity.geckolib_entity.model;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.VolcanicSerpentEntity.VolcanicSerpentTailEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

/** Tail view of the shared Volcanic Serpent geometry. */
public class VolcanicSerpentTailModel extends GeoModel<VolcanicSerpentTailEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "geo/volcanic_serpent.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "textures/entity/volcanic_serpent.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "animations/volcanic_serpent.animation.json");

    @Override public ResourceLocation getModelResource(VolcanicSerpentTailEntity entity) { return MODEL; }
    @Override public ResourceLocation getTextureResource(VolcanicSerpentTailEntity entity) { return TEXTURE; }
    @Override public ResourceLocation getAnimationResource(VolcanicSerpentTailEntity entity) { return ANIMATION; }

    @Override
    public void setCustomAnimations(VolcanicSerpentTailEntity entity, long instanceId, AnimationState<VolcanicSerpentTailEntity> state) {
        super.setCustomAnimations(entity, instanceId, state);
        CoreGeoBone all = getAnimationProcessor().getBone("all");
        CoreGeoBone head = getAnimationProcessor().getBone("head");
        CoreGeoBone body = getAnimationProcessor().getBone("body");
        CoreGeoBone tail = getAnimationProcessor().getBone("tail");
        CoreGeoBone tailExtension = getAnimationProcessor().getBone("bone10");

        setVisible(head, false);
        setVisible(body, false);
        setVisible(tail, true);
        setVisible(tailExtension, true);
        // Unified tail geometry is shifted +140 pixels relative to the former tail model.
        if (all != null) all.setPosZ(-140.0F);
        if (tail != null) {
            tail.setRotX(entity.getXRot() * ((float) Math.PI / 180.0F));
            tail.setRotZ(entity.getSerpentRoll());
        }
    }

    private static void setVisible(CoreGeoBone bone, boolean visible) {
        if (bone == null) return;
        bone.setHidden(!visible);
        bone.setChildrenHidden(!visible);
    }
}
