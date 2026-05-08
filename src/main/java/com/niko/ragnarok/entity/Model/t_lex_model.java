package com.niko.ragnarok.entity.Model;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.niko.ragnarok.entity.animations.t_lex_animation;
import com.niko.ragnarok.entity.costom.TLex;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public class t_lex_model<T extends Entity> extends HierarchicalModel<T> {

	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			ResourceLocation.fromNamespaceAndPath("ragnarok", "tlex_layer"), "main");
	private final ModelPart all;
	private final ModelPart leg2;
	private final ModelPart leg2_r1;
	private final ModelPart bone26;
	private final ModelPart cube_r3;
	private final ModelPart bone11;
	private final ModelPart bone2;
	private final ModelPart body;
	private final ModelPart bone;
	private final ModelPart head;
	private final ModelPart cube_r1;
	private final ModelPart bone25;
	private final ModelPart bone30;
	private final ModelPart bone18;
	private final ModelPart bone7;
	private final ModelPart arm1;
	private final ModelPart arm1_r1;
	private final ModelPart cube_r4;
	private final ModelPart arm2;
	private final ModelPart arm2_r1;
	private final ModelPart cube_r5;
	private final ModelPart teil;
	private final ModelPart bone12;
	private final ModelPart bone14;
	private final ModelPart bone15;
	private final ModelPart bone13;
	private final ModelPart bone16;
	private final ModelPart leg1;
	private final ModelPart leg2_r2;
	private final ModelPart bone3;
	private final ModelPart cube_r2;
	private final ModelPart bone4;
	private final ModelPart bone5;

	public t_lex_model(ModelPart root) {
		this.all = root.getChild("all");
		this.leg2 = this.all.getChild("leg2");
		this.leg2_r1 = this.leg2.getChild("leg2_r1");
		this.bone26 = this.leg2.getChild("bone26");
		this.cube_r3 = this.bone26.getChild("cube_r3");
		this.bone11 = this.bone26.getChild("bone11");
		this.bone2 = this.bone11.getChild("bone2");
		this.body = this.all.getChild("body");
		this.bone = this.body.getChild("bone");
		this.head = this.bone.getChild("head");
		this.cube_r1 = this.head.getChild("cube_r1");
		this.bone25 = this.cube_r1.getChild("bone25");
		this.bone30 = this.head.getChild("bone30");
		this.bone18 = this.bone30.getChild("bone18");
		this.bone7 = this.bone30.getChild("bone7");
		this.arm1 = this.bone.getChild("arm1");
		this.arm1_r1 = this.arm1.getChild("arm1_r1");
		this.cube_r4 = this.arm1.getChild("cube_r4");
		this.arm2 = this.bone.getChild("arm2");
		this.arm2_r1 = this.arm2.getChild("arm2_r1");
		this.cube_r5 = this.arm2.getChild("cube_r5");
		this.teil = this.body.getChild("teil");
		this.bone12 = this.teil.getChild("bone12");
		this.bone14 = this.bone12.getChild("bone14");
		this.bone15 = this.bone14.getChild("bone15");
		this.bone13 = this.bone15.getChild("bone13");
		this.bone16 = this.bone13.getChild("bone16");
		this.leg1 = this.all.getChild("leg1");
		this.leg2_r2 = this.leg1.getChild("leg2_r2");
		this.bone3 = this.leg1.getChild("bone3");
		this.cube_r2 = this.bone3.getChild("cube_r2");
		this.bone4 = this.bone3.getChild("bone4");
		this.bone5 = this.bone4.getChild("bone5");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition all = partdefinition.addOrReplaceChild("all", CubeListBuilder.create(), PartPose.offset(-13.0F, -21.0F, 11.0F));

		PartDefinition leg2 = all.addOrReplaceChild("leg2", CubeListBuilder.create(), PartPose.offset(-1.0F, -4.0F, -3.0F));

		PartDefinition leg2_r1 = leg2.addOrReplaceChild("leg2_r1", CubeListBuilder.create().texOffs(0, 93).addBox(-12.0F, -9.0359F, -10.5981F, 12.0F, 28.0F, 20.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.0F, 3.0F, 3.0F, -0.5236F, 0.0F, 0.0F));

		PartDefinition bone26 = leg2.addOrReplaceChild("bone26", CubeListBuilder.create(), PartPose.offset(-5.0F, 19.0F, 1.0F));

		PartDefinition cube_r3 = bone26.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(130, 185).addBox(-4.5F, -0.4833F, -4.8612F, 9.0F, 17.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, -1.0F, -1.5F, 0.6109F, 0.0F, 0.0F));

		PartDefinition bone11 = bone26.addOrReplaceChild("bone11", CubeListBuilder.create(), PartPose.offset(0.5F, 11.0F, 6.5F));

		PartDefinition bone11_r1 = bone11.addOrReplaceChild("bone11_r1", CubeListBuilder.create().texOffs(56, 189).addBox(-3.5F, -3.0F, -3.5F, 7.0F, 18.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.0F, 1.0F, -0.2182F, 0.0F, 0.0F));

		PartDefinition bone2 = bone11.addOrReplaceChild("bone2", CubeListBuilder.create().texOffs(166, 37).addBox(-5.5F, 0.5F, -14.0F, 11.0F, 5.0F, 20.0F, new CubeDeformation(0.0F))
		.texOffs(102, 141).addBox(-4.5F, 2.5F, -18.0F, 2.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(110, 86).addBox(-1.5F, 2.5F, -18.0F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(114, 141).addBox(2.5F, 2.5F, -18.0F, 2.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 13.5F, -1.5F));

		PartDefinition body = all.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-13.0F, -11.75F, 0.0F, 26.0F, 24.0F, 24.0F, new CubeDeformation(0.0F)), PartPose.offset(13.0F, -2.25F, -11.0F));

		PartDefinition bone = body.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(0, 48).addBox(-12.0F, -10.0F, -24.0F, 24.0F, 21.0F, 24.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -0.75F, 0.0F));

		PartDefinition head = bone.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -2.0F, -23.0F, 0.2182F, 0.0F, 0.0F));

		PartDefinition cube_r1 = head.addOrReplaceChild("cube_r1", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -0.6866F, -0.7273F, -0.6981F, 0.0F, 0.0F));

		PartDefinition bone25 = cube_r1.addOrReplaceChild("bone25", CubeListBuilder.create().texOffs(100, 0).addBox(-7.0F, -8.0F, -18.5F, 14.0F, 16.0F, 21.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.6837F, 3.5154F));

		PartDefinition bone30 = head.addOrReplaceChild("bone30", CubeListBuilder.create(), PartPose.offset(0.0F, -7.0F, -8.0F));

		PartDefinition bone18 = bone30.addOrReplaceChild("bone18", CubeListBuilder.create().texOffs(0, 174).addBox(-8.0F, -66.1866F, -42.2273F, 16.0F, 16.0F, 12.0F, new CubeDeformation(0.0F))
		.texOffs(0, 202).addBox(-8.0F, -66.1866F, -42.2273F, 16.0F, 16.0F, 12.0F, new CubeDeformation(0.0F))
		.texOffs(144, 37).addBox(-9.0F, -64.0F, -40.2F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(102, 148).addBox(8.0F, -64.0F, -40.2F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(130, 156).addBox(-7.0F, -65.1866F, -59.2273F, 14.0F, 12.0F, 17.0F, new CubeDeformation(0.0F))
		.texOffs(124, 92).addBox(-2.0F, -53.2F, -58.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F))
		.texOffs(140, 46).addBox(1.0F, -53.2F, -58.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F))
		.texOffs(126, 92).addBox(4.0F, -53.2F, -58.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F))
		.texOffs(124, 86).addBox(6.0F, -53.2F, -58.0F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(124, 88).addBox(6.0F, -53.2F, -56.0F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(124, 90).addBox(6.0F, -53.2F, -53.0F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(126, 86).addBox(6.0F, -53.2F, -50.0F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(126, 88).addBox(6.0F, -53.2F, -47.0F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(126, 90).addBox(-6.0F, -53.2F, -58.0F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(128, 44).addBox(-6.0F, -53.2F, -56.0F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(128, 46).addBox(-6.0F, -53.2F, -50.0F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(130, 44).addBox(-6.0F, -53.2F, -53.0F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(130, 46).addBox(-6.0F, -53.2F, -47.0F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(140, 44).addBox(-5.0F, -53.2F, -58.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 57.0F, 31.0F));

		PartDefinition bone7 = bone30.addOrReplaceChild("bone7", CubeListBuilder.create().texOffs(132, 44).addBox(4.0F, -2.3134F, -14.2727F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(132, 46).addBox(4.0F, -2.3134F, -11.2727F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(136, 44).addBox(4.0F, -2.3134F, -8.2727F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(134, 44).addBox(-4.0F, -2.3134F, -8.2727F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(138, 44).addBox(-4.0F, -2.3134F, -5.2727F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(138, 46).addBox(4.0F, -2.3134F, -5.2727F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(136, 46).addBox(-4.0F, -2.3134F, -11.2727F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(134, 46).addBox(-4.0F, -2.3134F, -14.2727F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(140, 45).addBox(-4.0F, -2.3134F, -14.2727F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F))
		.texOffs(126, 141).addBox(-2.0F, -2.3134F, -14.2727F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F))
		.texOffs(166, 62).addBox(-5.0F, -1.5F, -15.5F, 10.0F, 2.0F, 15.0F, new CubeDeformation(0.0F))
		.texOffs(140, 47).addBox(1.0F, -2.3134F, -14.2727F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F))
		.texOffs(142, 44).addBox(3.0F, -2.3134F, -14.2727F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 5.3134F, -10.7273F));

		PartDefinition arm1 = bone.addOrReplaceChild("arm1", CubeListBuilder.create(), PartPose.offset(13.6667F, 7.5F, -18.0F));

		PartDefinition arm1_r1 = arm1.addOrReplaceChild("arm1_r1", CubeListBuilder.create().texOffs(66, 141).addBox(-2.0F, -4.5F, -2.5F, 4.0F, 9.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.3333F, 0.0F, 0.5F, 0.6109F, 0.0F, 0.0F));

		PartDefinition cube_r4 = arm1.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(120, 44).addBox(-2.5F, 8.0F, 1.0F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(100, 37).addBox(-2.5F, 0.0F, -1.0F, 3.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.8333F, 2.5F, 2.0F, -0.6109F, 0.0F, 0.0F));

		PartDefinition arm2 = bone.addOrReplaceChild("arm2", CubeListBuilder.create(), PartPose.offset(-13.6667F, 7.5F, -18.0F));

		PartDefinition arm2_r1 = arm2.addOrReplaceChild("arm2_r1", CubeListBuilder.create().texOffs(84, 141).addBox(-2.0F, -4.5F, -2.5F, 4.0F, 9.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.3333F, 0.0F, 0.5F, 0.6109F, 0.0F, 0.0F));

		PartDefinition cube_r5 = arm2.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(120, 46).addBox(-0.5F, 8.0F, 1.0F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(110, 37).addBox(-0.5F, 0.0F, -1.0F, 3.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.8333F, 2.5F, 2.0F, -0.6109F, 0.0F, 0.0F));

		PartDefinition teil = body.addOrReplaceChild("teil", CubeListBuilder.create().texOffs(96, 48).addBox(-9.0F, -10.3333F, 0.3333F, 18.0F, 21.0F, 17.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -0.4167F, 23.6667F));

		PartDefinition bone12 = teil.addOrReplaceChild("bone12", CubeListBuilder.create(), PartPose.offset(0.0F, 0.1667F, 17.8333F));

		PartDefinition bone14 = bone12.addOrReplaceChild("bone14", CubeListBuilder.create().texOffs(128, 86).addBox(-8.0F, -9.0F, -0.5F, 16.0F, 18.0F, 18.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -0.5F, -1.0F));

		PartDefinition bone15 = bone14.addOrReplaceChild("bone15", CubeListBuilder.create().texOffs(66, 156).addBox(-7.0F, -7.5F, -1.0F, 14.0F, 15.0F, 18.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -0.5F, 17.5F));

		PartDefinition bone13 = bone15.addOrReplaceChild("bone13", CubeListBuilder.create().texOffs(0, 141).addBox(-6.0F, -6.0F, -1.0F, 12.0F, 12.0F, 21.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -0.5F, 17.0F));

		PartDefinition bone16 = bone13.addOrReplaceChild("bone16", CubeListBuilder.create().texOffs(128, 122).addBox(-5.0F, -3.836F, -1.0799F, 10.0F, 9.0F, 25.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -0.5F, 20.0F));

		PartDefinition leg1 = all.addOrReplaceChild("leg1", CubeListBuilder.create(), PartPose.offset(27.0F, -4.0F, -3.0F));

		PartDefinition leg2_r2 = leg1.addOrReplaceChild("leg2_r2", CubeListBuilder.create().texOffs(64, 93).addBox(0.0F, -9.0359F, -10.5981F, 12.0F, 28.0F, 20.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, 3.0F, 3.0F, -0.5236F, 0.0F, 0.0F));

		PartDefinition bone3 = leg1.addOrReplaceChild("bone3", CubeListBuilder.create(), PartPose.offset(5.0F, 19.0F, 1.0F));

		PartDefinition cube_r2 = bone3.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(170, 185).addBox(-4.5F, -0.4833F, -4.8612F, 9.0F, 17.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5F, -1.0F, -1.5F, 0.6109F, 0.0F, 0.0F));

		PartDefinition bone4 = bone3.addOrReplaceChild("bone4", CubeListBuilder.create(), PartPose.offset(-0.5F, 11.0F, 6.5F));

		PartDefinition bone11_r2 = bone4.addOrReplaceChild("bone11_r2", CubeListBuilder.create().texOffs(84, 189).addBox(-3.5F, -3.0F, -3.5F, 7.0F, 18.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.0F, 1.0F, -0.2182F, 0.0F, 0.0F));

		PartDefinition bone5 = bone4.addOrReplaceChild("bone5", CubeListBuilder.create().texOffs(170, 0).addBox(-5.5F, 0.5F, -14.0F, 11.0F, 5.0F, 20.0F, new CubeDeformation(0.0F))
		.texOffs(96, 86).addBox(-1.5F, 2.5F, -18.0F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(120, 37).addBox(-4.5F, 2.5F, -18.0F, 2.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(132, 37).addBox(2.5F, 2.5F, -18.0F, 2.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 13.5F, -1.5F));

		return LayerDefinition.create(meshdefinition, 256, 256);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.root().getAllParts().forEach(ModelPart::resetPose);
		this.applyHeadRotation(netHeadYaw, headPitch, ageInTicks);

		this.animateWalk(t_lex_animation.WALK, limbSwing, limbSwingAmount, 2f, 2.5f);
		this.animate(((TLex) entity).idleAnimationState, t_lex_animation.IDLE, ageInTicks, 1f);
		this.animate(((TLex) entity).attackAnimationState, t_lex_animation.ATTACK, ageInTicks, 1f);
		this.animate(((TLex) entity).roarAnimationState, t_lex_animation.ROAR, ageInTicks, 1f);
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