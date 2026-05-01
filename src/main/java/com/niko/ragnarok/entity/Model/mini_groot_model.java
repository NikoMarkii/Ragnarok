package com.niko.ragnarok.entity.Model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.niko.ragnarok.entity.animation.GrootAnimation;
import com.niko.ragnarok.entity.animation.mini_groot_animation;
import com.niko.ragnarok.entity.costom.Mini_Groot;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class mini_groot_model<T extends Entity> extends HierarchicalModel<T> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("ragnarok", "minigroot_model"), "main");

	private final ModelPart root;
	private final ModelPart all;
	private final ModelPart body;
	private final ModelPart arm;
	private final ModelPart arm2;
	private final ModelPart head;
	private final ModelPart bone;
	private final ModelPart leg2;
	private final ModelPart leg1;

	public mini_groot_model(ModelPart root) {
		this.root = root;
		this.all = root.getChild("all");
		this.body = this.all.getChild("body");
		this.arm = this.body.getChild("arm");
		this.arm2 = this.body.getChild("arm2");
		this.head = this.body.getChild("head");
		this.bone = this.all.getChild("bone");
		this.leg2 = this.bone.getChild("leg2");
		this.leg1 = this.bone.getChild("leg1");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition all = partdefinition.addOrReplaceChild("all", CubeListBuilder.create(), PartPose.offset(0.0F, 3.0F, 0.0F));

		PartDefinition body = all.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -9.0F, -5.0F, 14.0F, 9.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition arm = body.addOrReplaceChild("arm", CubeListBuilder.create().texOffs(44, 37).addBox(-1.3289F, -2.0163F, -3.0F, 5.0F, 19.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, -5.5F, 0.0F, 0.0F, 0.0F, -0.4363F));

		PartDefinition arm2 = body.addOrReplaceChild("arm2", CubeListBuilder.create().texOffs(0, 56).addBox(-3.6711F, -2.0163F, -3.0F, 5.0F, 19.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.0F, -5.5F, 0.0F, 0.0F, 0.0F, 0.4363F));

		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 19).addBox(-5.0F, -10.0F, -6.0F, 10.0F, 10.0F, 12.0F, new CubeDeformation(0.0F))
		.texOffs(0, 41).addBox(-5.0F, -13.0F, -6.0F, 10.0F, 3.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -9.0F, 0.0F));

		PartDefinition bone = all.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(44, 19).addBox(-6.0F, 0.0F, -4.0F, 12.0F, 10.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition leg2 = bone.addOrReplaceChild("leg2", CubeListBuilder.create().texOffs(22, 56).addBox(-2.5F, -0.5F, -3.0F, 5.0F, 11.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(3.5F, 10.5F, 0.0F));

		PartDefinition leg1 = bone.addOrReplaceChild("leg1", CubeListBuilder.create().texOffs(48, 0).addBox(-2.5F, -0.5F, -3.0F, 5.0F, 11.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(-3.5F, 10.5F, 0.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		// 1. まずはポーズをリセット（これを行わないとアニメが重なって大変なことになる）
		this.root().getAllParts().forEach(ModelPart::resetPose);

		// 2. エンティティが Mini_Groot であることを確認してアニメーションを適用[cite: 1, 2]
		if (entity instanceof Mini_Groot miniGroot) {
			// 頭の向きをマウスに合わせる（これだけは手動でやるのが通例だ）
			this.head.xRot = headPitch * ((float)Math.PI / 180F);
			this.head.yRot = netHeadYaw * ((float)Math.PI / 180F);

			// 各 AnimationState に対応する定義を流し込む[cite: 1, 2]
			// animate メソッドは実際にはこのように静的に呼び出すことが多い
			this.animate(miniGroot.idleAnimationState, mini_groot_animation.idle, ageInTicks);
			this.animateWalk(mini_groot_animation.walk, limbSwing, limbSwingAmount, 1.5f, 2.5f);
			this.animate(miniGroot.sitAnimationState, mini_groot_animation.sit, ageInTicks);
			this.animate(miniGroot.attackAnimationState, mini_groot_animation.attack, ageInTicks);

			this.animate(miniGroot.spinStartAnimationState, mini_groot_animation.spin_start, ageInTicks);
			this.animate(miniGroot.spinLoopAnimationState, mini_groot_animation.spin_loop, ageInTicks);
			this.animate(miniGroot.spinEndAnimationState, mini_groot_animation.spin_end, ageInTicks);
		}
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		all.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
	public ModelPart root() {
		return this.all;
	}
}