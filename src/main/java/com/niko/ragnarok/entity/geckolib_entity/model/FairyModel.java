package com.niko.ragnarok.entity.geckolib_entity.model;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.geckolib_entity.Costom.Fairy.FairyEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

/**
 * フェアリーのGeckoLibモデル
 */
public class FairyModel extends GeoModel<FairyEntity> {

    @Override
    public ResourceLocation getModelResource(FairyEntity fairyEntity) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "geo/fairy.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(FairyEntity fairyEntity) {
        // バリエーションに応じてテクスチャを切り替え
        int variant = fairyEntity.getVariant();
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID,
                "textures/entity/fairy/fairy" + (variant + 1) + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(FairyEntity fairyEntity) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "animations/fairy.animation.json");
    }
    @Override
    public void setCustomAnimations(FairyEntity fairyEntity, long instanceId, AnimationState<FairyEntity> animationState) {
        super.setCustomAnimations(fairyEntity, instanceId, animationState);

        CoreGeoBone head = getAnimationProcessor().getBone("head");

        if (head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);

            // 死亡していない限り、視線に合わせて首を動かす
            // Fairyクラスに isActuallyDying() メソッドがない場合は fairy.isAlive() などで代用してね
            if (fairyEntity.isAlive()) {
                head.setRotX(entityData.headPitch() * ((float) Math.PI / 180F));
                head.setRotY(entityData.netHeadYaw() * ((float) Math.PI / 180F));
            }
        }
    }
}