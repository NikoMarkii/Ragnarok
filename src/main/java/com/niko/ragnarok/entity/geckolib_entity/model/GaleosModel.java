package com.niko.ragnarok.entity.geckolib_entity.model;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.geckolib_entity.Costom.GaleosEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GaleosModel extends GeoModel<GaleosEntity> {

    @Override
    public ResourceLocation getModelResource(GaleosEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "geo/galeos.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GaleosEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "textures/entity/galeos.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GaleosEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "animations/galeos.animation.json");
    }
}