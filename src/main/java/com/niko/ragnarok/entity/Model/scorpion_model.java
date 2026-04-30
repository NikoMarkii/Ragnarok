package com.niko.ragnarok.entity.Model;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.animation.scorpion_animation;
import com.niko.ragnarok.entity.costom.Scorpion;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class scorpion_model<T extends Scorpion> extends HierarchicalModel<T> {
	public static final ModelLayerLocation SCORPION_LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(Ragnarok.MOD_ID, "scorpion_layer"), "main");
	private final ModelPart all;
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart fang1;
	private final ModelPart fang2;
	private final ModelPart leg1;
	private final ModelPart leg2;
	private final ModelPart tail;
	private final ModelPart tail2;
	private final ModelPart tail3;
	private final ModelPart tail4;
	private final ModelPart leg3;
	private final ModelPart leg4;
	private final ModelPart leg5;
	private final ModelPart leg6;
	private final ModelPart leg7;
	private final ModelPart leg8;
	private final ModelPart leg9;
	private final ModelPart leg10;

	public scorpion_model(ModelPart root) {
		this.all = root.getChild("all");
		this.body = this.all.getChild("body");
		this.head = this.body.getChild("head");
		this.fang1 = this.head.getChild("fang1");
		this.fang2 = this.head.getChild("fang2");
		this.leg1 = this.head.getChild("leg1");
		this.leg2 = this.head.getChild("leg2");
		this.tail = this.body.getChild("tail");
		this.tail2 = this.tail.getChild("tail2");
		this.tail3 = this.tail2.getChild("tail3");
		this.tail4 = this.tail3.getChild("tail4");
		this.leg3 = this.all.getChild("leg3");
		this.leg4 = this.all.getChild("leg4");
		this.leg5 = this.all.getChild("leg5");
		this.leg6 = this.all.getChild("leg6");
		this.leg7 = this.all.getChild("leg7");
		this.leg8 = this.all.getChild("leg8");
		this.leg9 = this.all.getChild("leg9");
		this.leg10 = this.all.getChild("leg10");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition all = partdefinition.addOrReplaceChild("all", CubeListBuilder.create(), PartPose.offset(0.0F, 13.0F, 0.0F));

		PartDefinition body = all.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -13.0F, -7.0F, 16.0F, 13.0F, 31.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, -6.0F, -6.0F));

		PartDefinition cube_r1 = head.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(56, 178).addBox(-7.0F, -2.0F, -10.0F, 14.0F, 13.0F, 12.0F, new CubeDeformation(0.0F))
		.texOffs(56, 100).addBox(-7.0F, -2.0F, -10.0F, 14.0F, 13.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -5.0F, -3.0F, 0.3927F, 0.0F, 0.0F));

		PartDefinition fang1 = head.addOrReplaceChild("fang1", CubeListBuilder.create(), PartPose.offsetAndRotation(4.5F, 4.1575F, -8.2779F, 0.0F, 0.2618F, 0.3491F));

		PartDefinition cube_r2 = fang1.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(32, 102).addBox(2.0F, 1.0F, -3.0F, 4.0F, 8.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.4647F, -4.1575F, -5.5858F, 0.3927F, 0.0F, 0.0F));

		PartDefinition fang2 = head.addOrReplaceChild("fang2", CubeListBuilder.create(), PartPose.offsetAndRotation(-4.5F, 4.1575F, -8.2779F, 0.0F, -0.2618F, -0.3491F));

		PartDefinition cube_r3 = fang2.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(32, 129).addBox(-6.0F, 1.0F, -3.0F, 4.0F, 8.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.4647F, -4.1575F, -5.5858F, 0.3927F, 0.0F, 0.0F));

		PartDefinition leg1 = head.addOrReplaceChild("leg1", CubeListBuilder.create(), PartPose.offset(6.0F, 2.0F, -5.0F));

		PartDefinition cube_r4 = leg1.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(56, 44).addBox(-6.0F, -7.0F, -13.0F, 9.0F, 11.0F, 19.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(14.0F, 7.0F, -2.0F, 0.0F, -0.3491F, 0.3927F));

		PartDefinition cube_r5 = leg1.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(94, 25).addBox(-5.0F, -4.0F, -2.0F, 15.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.0F, 2.0F, 0.0F, 0.0F, -0.3491F, 0.3927F));

		PartDefinition leg2 = head.addOrReplaceChild("leg2", CubeListBuilder.create(), PartPose.offset(-6.0F, 2.0F, -5.0F));

		PartDefinition cube_r6 = leg2.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(108, 100).addBox(-10.0F, -4.0F, -2.0F, 15.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, 2.0F, 0.0F, 0.0F, 0.3491F, -0.3927F));

		PartDefinition cube_r7 = leg2.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(0, 44).addBox(-3.0F, -7.0F, -13.0F, 9.0F, 11.0F, 19.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-14.0F, 7.0F, -2.0F, 0.0F, 0.3491F, -0.3927F));

		PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.offset(0.0F, -8.0F, 23.0F));

		PartDefinition cube_r8 = tail.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(56, 74).addBox(-6.0F, -5.5F, 2.5F, 12.0F, 11.0F, 15.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.5F, -3.5F, 0.3927F, 0.0F, 0.0F));

		PartDefinition tail2 = tail.addOrReplaceChild("tail2", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -4.1355F, 11.2769F, 0.6545F, 0.0F, 0.0F));

		PartDefinition cube_r9 = tail2.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(94, 0).addBox(-5.0F, -3.5F, 0.5F, 10.0F, 9.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.1796F, -2.124F, 0.3927F, 0.0F, 0.0F));

		PartDefinition tail3 = tail2.addOrReplaceChild("tail3", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -5.8151F, 10.6529F, 0.6545F, 0.0F, 0.0F));

		PartDefinition cube_r10 = tail3.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(0, 74).addBox(-4.0F, -2.5F, 0.5F, 8.0F, 8.0F, 20.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.3863F, -2.7327F, 0.3927F, 0.0F, 0.0F));

		PartDefinition tail4 = tail3.addOrReplaceChild("tail4", CubeListBuilder.create(), PartPose.offsetAndRotation(-1.0F, -8.0217F, 14.0441F, 1.1345F, 0.0F, 0.0F));

		PartDefinition cube_r11 = tail4.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(0, 102).addBox(1.0F, -2.5F, 0.5F, 0.0F, 9.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.8961F, -2.89F, 0.6109F, 0.0F, 0.0F));

		PartDefinition leg3 = all.addOrReplaceChild("leg3", CubeListBuilder.create(), PartPose.offset(6.25F, -2.75F, 1.0F));

		PartDefinition cube_r12 = leg3.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(112, 67).addBox(-2.2204F, -3.0678F, 0.8223F, 15.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(8.755F, 3.4663F, -5.0463F, 0.1201F, 0.2815F, 1.4133F));

		PartDefinition cube_r13 = leg3.addOrReplaceChild("cube_r13", CubeListBuilder.create().texOffs(112, 51).addBox(-4.0F, -3.0F, -1.0F, 14.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.75F, 0.75F, -1.0F, 0.0F, 0.3054F, 0.3054F));

		PartDefinition leg4 = all.addOrReplaceChild("leg4", CubeListBuilder.create(), PartPose.offset(-6.25F, -2.75F, 1.0F));

		PartDefinition cube_r14 = leg4.addOrReplaceChild("cube_r14", CubeListBuilder.create().texOffs(108, 118).addBox(-12.7796F, -3.0678F, 0.8223F, 15.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.755F, 3.4663F, -5.0463F, 0.1201F, -0.2815F, -1.4133F));

		PartDefinition cube_r15 = leg4.addOrReplaceChild("cube_r15", CubeListBuilder.create().texOffs(112, 59).addBox(-10.0F, -3.0F, -1.0F, 14.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.75F, 0.75F, -1.0F, 0.0F, -0.3054F, -0.3054F));

		PartDefinition leg5 = all.addOrReplaceChild("leg5", CubeListBuilder.create(), PartPose.offsetAndRotation(7.25F, -2.75F, 6.0F, 0.1309F, -0.3927F, 0.0F));

		PartDefinition cube_r16 = leg5.addOrReplaceChild("cube_r16", CubeListBuilder.create().texOffs(64, 125).addBox(-0.2466F, -0.052F, -1.0068F, 14.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(10.0661F, 1.9232F, -2.4922F, 0.2357F, 0.1875F, 1.318F));

		PartDefinition cube_r17 = leg5.addOrReplaceChild("cube_r17", CubeListBuilder.create().texOffs(94, 35).addBox(-4.0F, -3.0F, -1.0F, 14.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.6222F, -0.0235F, -0.8753F, 0.0F, 0.2182F, 0.3054F));

		PartDefinition leg6 = all.addOrReplaceChild("leg6", CubeListBuilder.create(), PartPose.offsetAndRotation(-7.25F, -2.75F, 6.0F, 0.1309F, 0.3927F, 0.0F));

		PartDefinition cube_r18 = leg6.addOrReplaceChild("cube_r18", CubeListBuilder.create().texOffs(96, 126).addBox(-13.7534F, -0.052F, -1.0068F, 14.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-10.0661F, 1.9232F, -2.4922F, 0.2357F, -0.1875F, -1.318F));

		PartDefinition cube_r19 = leg6.addOrReplaceChild("cube_r19", CubeListBuilder.create().texOffs(110, 74).addBox(-10.0F, -3.0F, -1.0F, 14.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.6222F, -0.0235F, -0.8753F, 0.0F, -0.2182F, -0.3054F));

		PartDefinition leg7 = all.addOrReplaceChild("leg7", CubeListBuilder.create(), PartPose.offsetAndRotation(7.25F, -2.75F, 13.0F, 0.1309F, -0.3927F, 0.0F));

		PartDefinition cube_r20 = leg7.addOrReplaceChild("cube_r20", CubeListBuilder.create().texOffs(0, 127).addBox(-0.2466F, -0.052F, -1.4831F, 14.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(10.0661F, 1.9232F, -1.4922F, 0.0405F, -0.1054F, 1.3208F));

		PartDefinition cube_r21 = leg7.addOrReplaceChild("cube_r21", CubeListBuilder.create().texOffs(110, 82).addBox(-4.0F, -3.0F, -1.0F, 14.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.6222F, -0.0235F, -0.8753F, 0.0F, 0.2182F, 0.3054F));

		PartDefinition leg8 = all.addOrReplaceChild("leg8", CubeListBuilder.create(), PartPose.offsetAndRotation(-7.25F, -2.75F, 13.0F, 0.1309F, 0.3927F, 0.0F));

		PartDefinition cube_r22 = leg8.addOrReplaceChild("cube_r22", CubeListBuilder.create().texOffs(110, 90).addBox(-10.0F, -3.0F, -1.0F, 14.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.6222F, -0.0235F, -0.8753F, 0.0F, -0.2182F, -0.3054F));

		PartDefinition cube_r23 = leg8.addOrReplaceChild("cube_r23", CubeListBuilder.create().texOffs(128, 126).addBox(-13.7534F, -0.052F, -1.4831F, 14.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-10.0661F, 1.9232F, -1.4922F, 0.0405F, 0.1054F, -1.3208F));

		PartDefinition leg9 = all.addOrReplaceChild("leg9", CubeListBuilder.create(), PartPose.offsetAndRotation(7.25F, -2.75F, 19.0F, 0.2096F, -0.9526F, -0.1213F));

		PartDefinition cube_r24 = leg9.addOrReplaceChild("cube_r24", CubeListBuilder.create().texOffs(108, 122).addBox(-0.2466F, -0.052F, -1.4831F, 14.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(9.2512F, 1.8026F, -2.0591F, 0.054F, -0.0993F, 1.1893F));

		PartDefinition cube_r25 = leg9.addOrReplaceChild("cube_r25", CubeListBuilder.create().texOffs(108, 110).addBox(-4.0F, -3.0F, -1.0F, 14.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.4371F, -0.1441F, -1.4422F, 0.0F, 0.2182F, 0.3054F));

		PartDefinition leg10 = all.addOrReplaceChild("leg10", CubeListBuilder.create(), PartPose.offsetAndRotation(-7.25F, -2.75F, 19.0F, 0.2096F, 0.9526F, 0.1213F));

		PartDefinition cube_r26 = leg10.addOrReplaceChild("cube_r26", CubeListBuilder.create().texOffs(32, 125).addBox(-13.7534F, -0.052F, -1.4831F, 14.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-9.2512F, 1.8026F, -2.0591F, 0.054F, 0.0993F, -1.1893F));

		PartDefinition cube_r27 = leg10.addOrReplaceChild("cube_r27", CubeListBuilder.create().texOffs(112, 43).addBox(-10.0F, -3.0F, -1.0F, 14.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.4371F, -0.1441F, -1.4422F, 0.0F, -0.2182F, -0.3054F));

		return LayerDefinition.create(meshdefinition, 256, 256);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.root().getAllParts().forEach(ModelPart::resetPose);
		this.applyHeadRotation(netHeadYaw, headPitch, ageInTicks);

		this.animateWalk(scorpion_animation.WALK, limbSwing, limbSwingAmount, 2f, 2.5f);
		this.animate(((Scorpion) entity).idleAnimationState, scorpion_animation.IDLE, ageInTicks, 1f);
		this.animate(((Scorpion) entity).attackAnimationState, scorpion_animation.ATTACK, ageInTicks, 1f);
	}

	private void applyHeadRotation(float pNetHeadYaw, float pHeadPitch, float pAgeInTicks) {
		pNetHeadYaw = Mth.clamp(pNetHeadYaw, -30.0F, 30.0F);
		pHeadPitch = Mth.clamp(pHeadPitch, -25.0F, 45.0F);

		this.head.yRot = pNetHeadYaw * ((float)Math.PI / 180F);
		this.head.xRot = pHeadPitch * ((float)Math.PI / 180F);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		all.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	@Override
	public ModelPart root() {
		return all;
	}
}