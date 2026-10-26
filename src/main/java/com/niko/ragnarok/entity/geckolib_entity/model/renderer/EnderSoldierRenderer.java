package com.niko.ragnarok.entity.geckolib_entity.model.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.niko.ragnarok.entity.geckolib_entity.Costom.EnderSoldierEntity;
import com.niko.ragnarok.entity.geckolib_entity.model.EnderSoldierModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class EnderSoldierRenderer extends GeoEntityRenderer<EnderSoldierEntity> {
    private static final ResourceLocation GLOW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("ragnarok", "textures/entity/ender_soldier2_glow.png");

    public EnderSoldierRenderer(EntityRendererProvider.Context context) {
        super(context, new EnderSoldierModel());
        this.shadowRadius = 1.1F;
        this.addRenderLayer(new EnderSoldierBeamLayer(this));
        this.addRenderLayer(new AfterimageLayer(this));
        this.addRenderLayer(new GlowLayer(this));
    }

    @Override
    protected float getDeathMaxRotation(EnderSoldierEntity entity) {
        return 0.0F;
    }

    private static class AfterimageLayer extends GeoRenderLayer<EnderSoldierEntity> {
        private static final int EMISSION_INTERVAL = 4;
        private static final int LIFETIME = 24;
        private static final double DRIFT_PER_TICK = 0.15D;
        private static final float INITIAL_OPACITY = 0.38F;
        private final Map<EnderSoldierEntity, Trail> trails = new WeakHashMap<>();

        AfterimageLayer(GeoEntityRenderer<EnderSoldierEntity> renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack poseStack, EnderSoldierEntity entity, BakedGeoModel model,
                           RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                           float partialTick, int packedLight, int packedOverlay) {
            if (!entity.isEnraged() || entity.isInvisible()) {
                trails.remove(entity);
                return;
            }
            float now = entity.tickCount + partialTick;
            Trail trail = trails.computeIfAbsent(entity, ignored -> new Trail(model));
            if (trail.model != model || (trail.lastPosition != null
                    && trail.lastPosition.distanceToSqr(entity.position()) > 16.0D)) {
                trail = new Trail(model);
                trails.put(entity, trail);
            }
            trail.lastPosition = entity.position();
            trail.images.removeIf(afterimage -> now - afterimage.createdAt >= LIFETIME);
            boolean emit = entity.isAlive() && now - trail.lastEmission >= EMISSION_INTERVAL;
            if (!emit && trail.images.isEmpty()) {
                return;
            }
            double bodyYaw = Math.toRadians(Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot));
            List<BonePose> currentPose = capturePose(model);
            if (emit) {
                trail.images.add(new Afterimage(now, currentPose));
                trail.lastEmission = now;
            }
            RenderType ghostType = RenderType.lightning();
            VertexConsumer ghostBuffer = new AfterimageVertices(bufferSource.getBuffer(ghostType));
            try {
                for (Afterimage afterimage : trail.images) {
                    float age = now - afterimage.createdAt;
                    double drift = 0.2D + age * DRIFT_PER_TICK;
                    float remaining = 1.0F - age / LIFETIME;
                    float opacity = INITIAL_OPACITY * remaining * remaining;
                    afterimage.pose.forEach(BonePose::apply);
                    poseStack.pushPose();
                    try {
                        poseStack.translate(Math.sin(bodyYaw) * drift, 0.0D, -Math.cos(bodyYaw) * drift);
                        getRenderer().reRender(model, poseStack, bufferSource, entity, ghostType,
                                ghostBuffer, partialTick, LightTexture.FULL_BRIGHT,
                                OverlayTexture.NO_OVERLAY, 0.85F, 0.65F, 1.0F, opacity);
                    } finally {
                        poseStack.popPose();
                    }
                }
            } finally {
                currentPose.forEach(BonePose::apply);
            }
        }

        // The additive lightning shader needs only position/color, not the dark skin texture or lighting.
        private record AfterimageVertices(VertexConsumer delegate) implements VertexConsumer {
            @Override
            public VertexConsumer vertex(double x, double y, double z) {
                delegate.vertex(x, y, z);
                return this;
            }

            @Override
            public VertexConsumer color(int red, int green, int blue, int alpha) {
                delegate.color(red, green, blue, alpha);
                return this;
            }

            @Override
            public VertexConsumer uv(float u, float v) {
                return this;
            }

            @Override
            public VertexConsumer overlayCoords(int u, int v) {
                return this;
            }

            @Override
            public VertexConsumer uv2(int u, int v) {
                return this;
            }

            @Override
            public VertexConsumer normal(float x, float y, float z) {
                return this;
            }

            @Override
            public void endVertex() {
                delegate.endVertex();
            }

            @Override
            public void defaultColor(int red, int green, int blue, int alpha) {
                delegate.defaultColor(red, green, blue, alpha);
            }

            @Override
            public void unsetDefaultColor() {
                delegate.unsetDefaultColor();
            }
        }

        private static List<BonePose> capturePose(BakedGeoModel model) {
            List<BonePose> pose = new ArrayList<>();
            for (GeoBone bone : model.topLevelBones()) {
                captureBone(bone, pose);
            }
            return pose;
        }

        private static void captureBone(GeoBone bone, List<BonePose> pose) {
            pose.add(new BonePose(bone, bone.getPosX(), bone.getPosY(), bone.getPosZ(),
                    bone.getRotX(), bone.getRotY(), bone.getRotZ(),
                    bone.getScaleX(), bone.getScaleY(), bone.getScaleZ(),
                    bone.hasPositionChanged(), bone.hasRotationChanged(), bone.hasScaleChanged()));
            for (GeoBone child : bone.getChildBones()) {
                captureBone(child, pose);
            }
        }

        private record BonePose(GeoBone bone, float posX, float posY, float posZ,
                                float rotX, float rotY, float rotZ, float scaleX, float scaleY, float scaleZ,
                                boolean positionChanged, boolean rotationChanged, boolean scaleChanged) {
            private void apply() {
                bone.setPosX(posX);
                bone.setPosY(posY);
                bone.setPosZ(posZ);
                bone.setRotX(rotX);
                bone.setRotY(rotY);
                bone.setRotZ(rotZ);
                bone.setScaleX(scaleX);
                bone.setScaleY(scaleY);
                bone.setScaleZ(scaleZ);
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
        }

        private record Afterimage(float createdAt, List<BonePose> pose) {
        }

        private static class Trail {
            private final BakedGeoModel model;
            private final List<Afterimage> images = new ArrayList<>();
            private float lastEmission = Float.NEGATIVE_INFINITY;
            private Vec3 lastPosition;

            private Trail(BakedGeoModel model) {
                this.model = model;
            }
        }
    }

    private static class GlowLayer extends GeoRenderLayer<EnderSoldierEntity> {
        GlowLayer(GeoEntityRenderer<EnderSoldierEntity> renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack poseStack, EnderSoldierEntity entity, BakedGeoModel model,
                           RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                           float partialTick, int packedLight, int packedOverlay) {
            if (entity.isInvisible()) {
                return;
            }
            RenderType glowType = RenderType.eyes(GLOW_TEXTURE);
            getRenderer().reRender(model, poseStack, bufferSource, entity, glowType,
                    bufferSource.getBuffer(glowType), partialTick, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
