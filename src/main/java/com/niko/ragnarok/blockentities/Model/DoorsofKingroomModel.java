package com.niko.ragnarok.blockentities.Model;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.blockentities.DoorsofkingroomBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DoorsofKingroomModel extends GeoModel<DoorsofkingroomBlockEntity> {
    @Override
    public ResourceLocation getModelResource(DoorsofkingroomBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "geo/doors_king_room.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DoorsofkingroomBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "textures/block/doors_king_room.png");
    }

    @Override
    public ResourceLocation getAnimationResource(DoorsofkingroomBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "animations/doors_king_room.animation.json");
    }
}
