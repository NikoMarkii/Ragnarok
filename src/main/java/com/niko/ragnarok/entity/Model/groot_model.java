package com.niko.ragnarok.entity.Model;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.niko.ragnarok.entity.animation.GrootAnimation;
import com.niko.ragnarok.entity.costom.Groot;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

public class groot_model<T extends Groot> extends HierarchicalModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("ragnarok", "groot"), "main");

	private final ModelPart root;
	private final ModelPart all;
	private final ModelPart body;
	private final ModelPart bone;
	private final ModelPart bone7;
	private final ModelPart arm2;
	private final ModelPart bone3;
	private final ModelPart arm1;
	private final ModelPart bone2;
	private final ModelPart head;
	private final ModelPart leg1;
	private final ModelPart bone4;
	private final ModelPart leg2;
	private final ModelPart bone5;

	public groot_model(ModelPart root) {
		this.root = root;
		this.all = root.getChild("all");
		this.body = this.all.getChild("body");
		this.bone = this.body.getChild("bone");
		this.bone7 = this.body.getChild("bone7");
		this.arm2 = this.bone7.getChild("arm2");
		this.bone3 = this.arm2.getChild("bone3");
		this.arm1 = this.bone7.getChild("arm1");
		this.bone2 = this.arm1.getChild("bone2");
		this.head = this.bone7.getChild("head");
		this.leg1 = this.all.getChild("leg1");
		this.bone4 = this.leg1.getChild("bone4");
		this.leg2 = this.all.getChild("leg2");
		this.bone5 = this.leg2.getChild("bone5");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition all = partdefinition.addOrReplaceChild("all", CubeListBuilder.create(), PartPose.offset(0.0F, -25.0F, 4.0F));

		PartDefinition body = all.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -4.0F, -0.25F, 0.3054F, 0.0F, 0.0F));

		PartDefinition bone = body.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(72, 33).addBox(-10.0F, -1.0463F, -8.3007F, 20.0F, 15.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 7.0F, 0.25F));

		PartDefinition bone7 = body.addOrReplaceChild("bone7", CubeListBuilder.create().texOffs(0, 0).addBox(-16.0F, -11.0463F, -9.0507F, 32.0F, 17.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition arm2 = bone7.addOrReplaceChild("arm2", CubeListBuilder.create().texOffs(32, 139).addBox(-4.5231F, -1.0939F, -3.8007F, 8.0F, 22.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-12.5F, -6.75F, -1.25F, 0.0F, 0.0F, 0.5236F));

		PartDefinition bone3 = arm2.addOrReplaceChild("bone3", CubeListBuilder.create().texOffs(0, 87).addBox(-5.0231F, 1.1734F, -1.4351F, 12.0F, 31.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.5F, 17.5801F, 5.5F, -1.0472F, 0.0F, 0.0F));

		PartDefinition arm1 = bone7.addOrReplaceChild("arm1", CubeListBuilder.create().texOffs(0, 130).addBox(-3.4769F, -1.0939F, -3.8007F, 8.0F, 22.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(12.5F, -6.75F, -1.25F, 0.0F, 0.0F, -0.5236F));

		PartDefinition bone2 = arm1.addOrReplaceChild("bone2", CubeListBuilder.create().texOffs(72, 62).addBox(-6.9769F, 1.1734F, -1.4351F, 12.0F, 31.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.5F, 17.5801F, 5.5F, -1.0472F, 0.0F, 0.0F));

		PartDefinition head = bone7.addOrReplaceChild("head", CubeListBuilder.create().texOffs(104, 139).addBox(-6.0F, -4.25F, -5.75F, 12.0F, 4.0F, 11.0F, new CubeDeformation(0.0F))
		.texOffs(64, 139).addBox(-5.0F, -11.25F, -4.75F, 10.0F, 11.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -10.7963F, -0.3007F));

		PartDefinition leg1 = all.addOrReplaceChild("leg1", CubeListBuilder.create().texOffs(96, 0).addBox(-7.25F, -0.9066F, -6.4627F, 11.0F, 19.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.75F, 14.75F, 5.0F, -0.5672F, 0.0F, 0.0F));

		PartDefinition bone4 = leg1.addOrReplaceChild("bone4", CubeListBuilder.create().texOffs(48, 105).addBox(-6.5F, 0.5F, -7.75F, 13.0F, 20.0F, 14.0F, new CubeDeformation(0.0F))
		.texOffs(0, 33).addBox(-7.5F, 16.5F, -13.75F, 15.0F, 6.0F, 21.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.75F, 14.75F, -0.25F, 0.5672F, 0.0F, 0.0F));

		PartDefinition leg2 = all.addOrReplaceChild("leg2", CubeListBuilder.create().texOffs(120, 62).addBox(-3.75F, -0.9066F, -6.4627F, 11.0F, 19.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.75F, 14.75F, 5.0F, -0.5672F, 0.0F, 0.0F));

		PartDefinition bone5 = leg2.addOrReplaceChild("bone5", CubeListBuilder.create().texOffs(102, 105).addBox(-6.5F, 0.5F, -7.75F, 13.0F, 20.0F, 14.0F, new CubeDeformation(0.0F))
		.texOffs(0, 60).addBox(-7.5F, 16.5F, -13.75F, 15.0F, 6.0F, 21.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.75F, 14.75F, -0.25F, 0.5672F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 256, 256);
	}
	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.root().getAllParts().forEach(ModelPart::resetPose);

		// 頭の回転
		this.head.yRot = netHeadYaw * ((float)Math.PI / 180F);
		this.head.xRot = headPitch * ((float)Math.PI / 180F);

		if (entity.walkAnimationState.isStarted()) {
			// 歩行中
			float walkTime = limbSwing * 0.8f;
			this.animate(entity.walkAnimationState, GrootAnimation.walk, walkTime, 1.0f);
		} else {
			// 待機中
			this.animate(entity.idleAnimationState, GrootAnimation.idea, ageInTicks, 1.0f);
		}

		this.animate(entity.attack1AnimationState, GrootAnimation.attack1, ageInTicks, 1.0F);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		all.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
	@Override
	public ModelPart root() {
		return this.root;
	}
}