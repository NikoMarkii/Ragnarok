package com.niko.ragnarok.entity.geckolib_entity.model;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.geckolib_entity.Costom.Ender_Soldier;
import com.niko.ragnarok.entity.geckolib_entity.Costom.Fairy;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

/**
 * フェアリーのGeckoLibモデル
 */
public class FairyModel extends GeoModel<Fairy> {

    @Override
    public ResourceLocation getModelResource(Fairy fairy) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "geo/fairy.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Fairy fairy) {
        // バリエーションに応じてテクスチャを切り替え
        int variant = fairy.getVariant();
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID,
                "textures/entity/fairy/fairy" + (variant + 1) + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(Fairy fairy) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "animations/fairy.animation.json");
    }
    @Override
    public void setCustomAnimations(Fairy fairy, long instanceId, AnimationState<Fairy> animationState) {
        super.setCustomAnimations(fairy, instanceId, animationState);

        CoreGeoBone head = getAnimationProcessor().getBone("head");

        if (head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);

            // 死亡していない限り、視線に合わせて首を動かす
            // Fairyクラスに isActuallyDying() メソッドがない場合は fairy.isAlive() などで代用してね
            if (fairy.isAlive()) {
                head.setRotX(entityData.headPitch() * ((float) Math.PI / 180F));
                head.setRotY(entityData.netHeadYaw() * ((float) Math.PI / 180F));
            }
        }
    }
}