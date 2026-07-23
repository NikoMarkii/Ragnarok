package com.niko.ragnarok.entity.geckolib_entity.renderer;

import com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.GradiusEntity;
import com.niko.ragnarok.entity.geckolib_entity.model.GradiusModel;
import com.niko.ragnarok.item.Ragnarok_mainItems;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public class GradiusRenderer extends GeoEntityRenderer<GradiusEntity> {

    public GradiusRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GradiusModel());
        this.shadowRadius = 1.5F;

        addRenderLayer(new BlockAndItemGeoLayer<>(this) {

            @Override
            protected ItemStack getStackForBone(
                    GeoBone bone,
                    GradiusEntity animatable) {

                if (bone.getName().equals("right_hand_item")) {
                    return new ItemStack(Ragnarok_mainItems.GRADIUS_GREAT_SWORD.get());
                }

                return ItemStack.EMPTY;
            }

            @Override
            protected ItemDisplayContext getTransformTypeForStack(
                    GeoBone bone,
                    ItemStack stack,
                    GradiusEntity animatable) {

                return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            }
        });
    }
}
