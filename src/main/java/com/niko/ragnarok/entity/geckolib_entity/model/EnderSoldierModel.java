package com.niko.ragnarok.entity.geckolib_entity.model;

import com.niko.ragnarok.entity.geckolib_entity.Costom.EnderSoldierEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.state.BoneSnapshot;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

public class EnderSoldierModel extends GeoModel<EnderSoldierEntity> {
    private static final float MOVEMENT_BLEND_TICKS = 6.0F;
    private static final float ACTION_BLEND_TICKS = 3.0F;
    private final Map<EnderSoldierEntity, PoseTransition> poseTransitions = new WeakHashMap<>();

    @Override
    public ResourceLocation getModelResource(EnderSoldierEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("ragnarok", "geo/ender_soldier2.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(EnderSoldierEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("ragnarok", "textures/entity/ender_soldier2.png");
    }

    @Override
    public ResourceLocation getAnimationResource(EnderSoldierEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("ragnarok", "animations/ender_soldier2.animation.json");
    }

    @Override
    public void setCustomAnimations(EnderSoldierEntity animatable, long instanceId, AnimationState<EnderSoldierEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        CoreGeoBone head = getAnimationProcessor().getBone("head");

        if (head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);

            if (entityData != null && animatable.isAlive() && animatable.getAttackState() == 0) {
                head.setRotX(head.getRotX() + entityData.headPitch() * ((float) Math.PI / 180F));
                head.setRotY(head.getRotY() + entityData.netHeadYaw() * ((float) Math.PI / 180F));
            }
        }
        blendAnimationTransition(animatable, animationState);
    }

    private void blendAnimationTransition(EnderSoldierEntity entity, AnimationState<EnderSoldierEntity> state) {
        if (state.getController() == null) {
            return;
        }
        RawAnimation animation = state.getController().getCurrentRawAnimation();
        PoseTransition transition = poseTransitions.computeIfAbsent(entity, ignored -> new PoseTransition());
        float now = entity.tickCount + state.getPartialTick();
        if (!Objects.equals(animation, transition.animation)) {
            transition.animation = animation;
            transition.startPose = transition.lastPose;
            transition.startedAt = now;
            transition.duration = entity.isDeadOrDying() || entity.getAttackState() != 0
                    ? ACTION_BLEND_TICKS : MOVEMENT_BLEND_TICKS;
        }
        float progress = Mth.clamp((now - transition.startedAt) / transition.duration, 0.0F, 1.0F);
        float blend = progress * progress * (3.0F - 2.0F * progress);
        Map<String, BoneSnapshot> displayedPose = new HashMap<>();
        for (CoreGeoBone bone : getAnimationProcessor().getRegisteredBones()) {
            BoneSnapshot previous = transition.startPose.get(bone.getName());
            if (previous != null && previous.getBone() == bone && progress < 1.0F) {
                blendBone(bone, previous, blend);
            }
            displayedPose.put(bone.getName(), new BoneSnapshot(bone));
        }
        transition.lastPose = displayedPose;
        if (progress >= 1.0F) {
            transition.startPose = Map.of();
        }
    }

    private static void blendBone(CoreGeoBone bone, BoneSnapshot previous, float blend) {
        boolean positionChanged = bone.hasPositionChanged();
        boolean rotationChanged = bone.hasRotationChanged();
        boolean scaleChanged = bone.hasScaleChanged();
        bone.setPosX(Mth.lerp(blend, previous.getOffsetX(), bone.getPosX()));
        bone.setPosY(Mth.lerp(blend, previous.getOffsetY(), bone.getPosY()));
        bone.setPosZ(Mth.lerp(blend, previous.getOffsetZ(), bone.getPosZ()));
        bone.setRotX(blendRotation(blend, previous.getRotX(), bone.getRotX()));
        bone.setRotY(blendRotation(blend, previous.getRotY(), bone.getRotY()));
        bone.setRotZ(blendRotation(blend, previous.getRotZ(), bone.getRotZ()));
        bone.setScaleX(Mth.lerp(blend, previous.getScaleX(), bone.getScaleX()));
        bone.setScaleY(Mth.lerp(blend, previous.getScaleY(), bone.getScaleY()));
        bone.setScaleZ(Mth.lerp(blend, previous.getScaleZ(), bone.getScaleZ()));
        bone.resetStateChanges();
        if (positionChanged) {
            bone.markPositionAsChanged();
        }
        if (rotationChanged) {
            bone.markRotationAsChanged();
        }
        if (scaleChanged) {
            bone.markScaleAsChanged();
        }
    }

    static float blendRotation(float blend, float from, float to) {
        // Preserve full turns inside attacks, but never unwind them between animations.
        float difference = Mth.wrapDegrees((to - from) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
        return from + blend * difference;
    }

    private static class PoseTransition {
        private RawAnimation animation;
        private Map<String, BoneSnapshot> startPose = Map.of();
        private Map<String, BoneSnapshot> lastPose = Map.of();
        private float startedAt;
        private float duration = MOVEMENT_BLEND_TICKS;
    }
}
