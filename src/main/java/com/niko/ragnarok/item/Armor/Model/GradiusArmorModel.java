package com.niko.ragnarok.item.Armor.Model;

import com.niko.ragnarok.item.Armor.GradiusArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GradiusArmorModel extends GeoModel<GradiusArmorItem> {

    @Override
    public ResourceLocation getModelResource(GradiusArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath("ragnarok", "geo/armor/gradius_armor.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GradiusArmorItem animatable) {
        // 4部位とも共通の1枚テクスチャ（UVがgeo.json側で各部位に振り分けられている）
        return ResourceLocation.fromNamespaceAndPath("ragnarok", "textures/models/armor/gradius_armor.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GradiusArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath("ragnarok", "animations/empty.animation.json");
    }
}
