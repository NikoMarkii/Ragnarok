package com.niko.ragnarok.entity.Model;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.niko.ragnarok.entity.animations.magic_golem_animation;
import com.niko.ragnarok.entity.costom.Magic_Golem;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

public class magic_golem_model<T extends Magic_Golem> extends HierarchicalModel<T> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			ResourceLocation.fromNamespaceAndPath("ragnarok", "magic_golem"), "main");

	private final ModelPart root;
	private final ModelPart all;
	private final ModelPart body;
	private final ModelPart bone;
	private final ModelPart right_leg;
	private final ModelPart bone6;
	private final ModelPart left_leg;
	private final ModelPart bone5;
	private final ModelPart bone2;
	private final ModelPart left_arm;
	private final ModelPart bone3;
	private final ModelPart right_arm;
	private final ModelPart bone4;
	private final ModelPart head;

	public magic_golem_model(ModelPart root) {
		this.root = root;
		this.all = root.getChild("all");
		this.body = this.all.getChild("body");
		this.bone = this.body.getChild("bone");
		this.right_leg = this.bone.getChild("right_leg");
		this.bone6 = this.right_leg.getChild("bone6");
		this.left_leg = this.bone.getChild("left_leg");
		this.bone5 = this.left_leg.getChild("bone5");
		this.bone2 = this.body.getChild("bone2");
		this.left_arm = this.bone2.getChild("left_arm");
		this.bone3 = this.left_arm.getChild("bone3");
		this.right_arm = this.bone2.getChild("right_arm");
		this.bone4 = this.right_arm.getChild("bone4");
		this.head = this.bone2.getChild("head");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition all = partdefinition.addOrReplaceChild("all", CubeListBuilder.create(), PartPose.offset(0.0F, 0.5F, -0.5F));

		PartDefinition body = all.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, -7.5F, 0.5F));

		PartDefinition bone = body.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(0, 70).addBox(-4.5F, 10.0F, -3.0F, 9.0F, 5.0F, 6.0F, new CubeDeformation(0.5F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition right_leg = bone.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(37, 0).addBox(-3.5F, 0.0F, -3.0F, 6.0F, 8.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(-4.0F, 15.0F, 0.0F));

		PartDefinition bone6 = right_leg.addOrReplaceChild("bone6", CubeListBuilder.create().texOffs(37, 15).addBox(-2.5F, 0.0F, -3.0F, 6.0F, 8.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.0F, 8.0F, 0.0F));

		PartDefinition left_leg = bone.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(60, 0).addBox(-2.5F, 0.0F, -3.0F, 6.0F, 8.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(4.0F, 15.0F, 0.0F));

		PartDefinition bone5 = left_leg.addOrReplaceChild("bone5", CubeListBuilder.create().texOffs(84, 0).addBox(-3.0F, 0.0F, -2.5F, 6.0F, 8.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.5F, 8.0F, -0.5F));

		PartDefinition bone2 = body.addOrReplaceChild("bone2", CubeListBuilder.create().texOffs(0, 40).addBox(-9.0F, -12.0F, -6.0F, 18.0F, 12.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 10.0F, 0.0F));

		PartDefinition left_arm = bone2.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(60, 58).addBox(0.0F, -2.5F, -3.0F, 4.0F, 13.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(9.0F, -10.0F, 0.0F));

		PartDefinition bone3 = left_arm.addOrReplaceChild("bone3", CubeListBuilder.create().texOffs(82, 58).addBox(-2.0F, 0.5F, -3.0F, 4.0F, 17.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(2.0F, 10.0F, 0.0F));

		PartDefinition right_arm = bone2.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(60, 21).addBox(-4.0F, -2.5F, -3.0F, 4.0F, 13.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(-9.0F, -10.0F, 0.0F));

		PartDefinition bone4 = right_arm.addOrReplaceChild("bone4", CubeListBuilder.create().texOffs(82, 21).addBox(-2.0F, 0.5F, -3.0F, 4.0F, 17.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.0F, 10.0F, 0.0F));

		PartDefinition head = bone2.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -12.0F, -5.5F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(24, 0).addBox(-1.0F, -5.0F, -7.5F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(0, 111).addBox(-5.0F, -13.0F, -6.0F, 10.0F, 8.0F, 9.0F, new CubeDeformation(0.0F))
		.texOffs(38, 115).addBox(5.0F, -21.0F, -2.0F, 2.0F, 11.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(60, 115).addBox(-7.0F, -21.0F, -2.0F, 2.0F, 11.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -10.0F, -2.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.root().getAllParts().forEach(ModelPart::resetPose);

		// 頭の向き
		this.head.yRot = netHeadYaw * ((float)Math.PI / 180F);
		this.head.xRot = headPitch * ((float)Math.PI / 180F);

		// アニメーション
		this.animateWalk(magic_golem_animation.walk, limbSwing, limbSwingAmount, 2.0f, 2.5f);
		this.animate(entity.idleAnimationState, magic_golem_animation.idle, ageInTicks, 1.0f);
		this.animate(entity.attack1AnimationState, magic_golem_animation.attack1, ageInTicks, 1.0f);
		this.animate(entity.attack2AnimationState, magic_golem_animation.attack2, ageInTicks, 1.0f);
		this.animate(entity.summonAnimationState, magic_golem_animation.summon, ageInTicks, 1.0f);
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