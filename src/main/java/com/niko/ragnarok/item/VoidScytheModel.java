package com.niko.ragnarok.item;

import com.niko.ragnarok.Ragnarok;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class VoidScytheModel extends GeoModel<VoidScythe> {
    @Override
    public ResourceLocation getModelResource(VoidScythe item) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "geo/item/voidscythe.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(VoidScythe item) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "textures/item/voidscythe.png");
    }

    @Override
    public ResourceLocation getAnimationResource(VoidScythe item) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "animations/empty.animation.json");
    }
}
