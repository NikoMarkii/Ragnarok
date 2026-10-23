package com.niko.ragnarok.entity.geckolib_entity.model.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.niko.ragnarok.entity.geckolib_entity.Costom.EnderSoldierEntity;
import com.niko.ragnarok.entity.others.EnderSoldierBeam;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EnderSoldierBeamLayer extends GeoRenderLayer<EnderSoldierEntity> {
    private static final int SIDES = 12;

    public EnderSoldierBeamLayer(GeoEntityRenderer<EnderSoldierEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, EnderSoldierEntity entity, BakedGeoModel model,
                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                       float partialTick, int packedLight, int packedOverlay) {
        if (!entity.isFiringBeam() || entity.isInvisible()) {
            return;
        }
        GeoBone mouth = model.getBone("mouth").orElse(null);
        if (mouth == null) {
            return;
        }
        Vec3 offset = mouthOffset(entity, mouth, partialTick);
        Vec3 origin = entity.getPosition(partialTick).add(offset);
        Vec3 displacement = entity.getBeamEnd(origin).subtract(origin);
        double length = displacement.length();
        if (length < 0.001D) {
            return;
        }
        float age = entity.getBeamTick() + partialTick;
        float envelope = Mth.clamp(Math.min((age - EnderSoldierBeam.START_TICK + 1.0F) / 3.0F,
                (EnderSoldierBeam.END_TICK - age) / 4.0F), 0.0F, 1.0F);
        float pulse = 0.95F + 0.05F * Mth.sin(age * 1.8F);
        double radius = EnderSoldierBeam.RADIUS * envelope * pulse;
        Vec3 direction = displacement.scale(1.0D / length);
        Vec3 reference = Math.abs(direction.y) > 0.95D ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 side = direction.cross(reference).normalize();
        Vec3 up = direction.cross(side).normalize();
        VertexConsumer vertices = bufferSource.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();
        drawCylinder(vertices, matrix, offset, displacement, side, up, radius, 0.6F, 0.1F, 1.0F, 0.3F * envelope);
        drawCylinder(vertices, matrix, offset, displacement, side, up, radius * 0.65D, 0.8F, 0.35F, 1.0F, 0.55F * envelope);
        drawCylinder(vertices, matrix, offset, displacement, side, up, radius * 0.3D, 1.0F, 0.9F, 1.0F, 0.95F * envelope);
    }

    private static Vec3 mouthOffset(EnderSoldierEntity entity, GeoBone mouth, float partialTick) {
        List<GeoBone> chain = new ArrayList<>();
        for (GeoBone bone = mouth; bone != null; bone = bone.getParent()) {
            chain.add(bone);
        }
        Collections.reverse(chain);
        PoseStack transform = new PoseStack();
        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        transform.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        transform.translate(0.0D, 0.01D, 0.0D);
        for (GeoBone bone : chain) {
            RenderUtils.prepMatrixForBone(transform, bone);
        }
        RenderUtils.translateToPivotPoint(transform, mouth);
        Vector3f position = transform.last().pose().transformPosition(new Vector3f());
        return new Vec3(position.x, position.y, position.z);
    }

    private static void drawCylinder(VertexConsumer vertices, Matrix4f matrix, Vec3 start, Vec3 displacement,
                                     Vec3 side, Vec3 up, double radius, float red, float green, float blue, float alpha) {
        Vec3 end = start.add(displacement);
        for (int segment = 0; segment < SIDES; segment++) {
            double angle = segment * Math.PI * 2.0D / SIDES;
            double nextAngle = (segment + 1) * Math.PI * 2.0D / SIDES;
            Vec3 first = side.scale(Math.cos(angle) * radius).add(up.scale(Math.sin(angle) * radius));
            Vec3 second = side.scale(Math.cos(nextAngle) * radius).add(up.scale(Math.sin(nextAngle) * radius));
            vertex(vertices, matrix, start.add(first), red, green, blue, alpha);
            vertex(vertices, matrix, end.add(first), red, green, blue, alpha);
            vertex(vertices, matrix, end.add(second), red, green, blue, alpha);
            vertex(vertices, matrix, start.add(second), red, green, blue, alpha);
            vertex(vertices, matrix, end, red, green, blue, alpha);
            vertex(vertices, matrix, end.add(second), red, green, blue, alpha);
            vertex(vertices, matrix, end.add(first), red, green, blue, alpha);
            vertex(vertices, matrix, end, red, green, blue, alpha);
        }
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, Vec3 position,
                               float red, float green, float blue, float alpha) {
        vertices.vertex(matrix, (float) position.x, (float) position.y, (float) position.z)
                .color(red, green, blue, alpha).endVertex();
    }
}
