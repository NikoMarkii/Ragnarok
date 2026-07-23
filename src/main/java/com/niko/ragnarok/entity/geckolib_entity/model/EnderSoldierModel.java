package com.niko.ragnarok.entity.geckolib_entity.model;

import com.niko.ragnarok.entity.geckolib_entity.Costom.EnderSoldierEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class EnderSoldierModel extends GeoModel<EnderSoldierEntity> {

    @Override
    public ResourceLocation getModelResource(EnderSoldierEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("ragnarok", "geo/ender_soldier.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(EnderSoldierEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("ragnarok", "textures/entity/ender_soldier.png");
    }

    @Override
    public ResourceLocation getAnimationResource(EnderSoldierEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("ragnarok", "animations/ender_soldier.animation.json");
    }

    @Override
    public void setCustomAnimations(EnderSoldierEntity animatable, long instanceId, AnimationState<EnderSoldierEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        CoreGeoBone head = getAnimationProcessor().getBone("head");

        if (head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);

            // 死亡していない限り、攻撃中であっても首を動かすように条件を変更
            if (!animatable.isActuallyDying()) {
                // アニメーションによる回転に、マイクラの視線回転を「加算」または「上書き」する
                // 基本的には上書きで問題ないが、Math.PI / 180F でラジアンに変換するよ
                head.setRotX(entityData.headPitch() * ((float) Math.PI / 180F));
                head.setRotY(entityData.netHeadYaw() * ((float) Math.PI / 180F));
            }
        }
    }
}
