package com.niko.ragnarok.item.Gradius_Great_Sword;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.item.Gradius_Great_Sword.GradiusGreatSword;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GradiusGreatSwordModel extends GeoModel<GradiusGreatSword> {

    @Override
    public ResourceLocation getModelResource(GradiusGreatSword item) {
        return ResourceLocation.fromNamespaceAndPath(
                Ragnarok.MOD_ID, "geo/item/gradius_great_sword.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GradiusGreatSword item) {
        return ResourceLocation.fromNamespaceAndPath(
                Ragnarok.MOD_ID, "textures/item/gradius_great_sword.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GradiusGreatSword item) {
        return ResourceLocation.fromNamespaceAndPath(
                Ragnarok.MOD_ID, "animations/empty.animation.json");
    }
}
