package com.niko.ragnarok.entity.geckolib_entity.model;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.VolcanicSerpent.VolcanicSerpentHeadEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class VolcanicSerpentModel extends GeoModel<VolcanicSerpentHeadEntity> {
    @Override
    public ResourceLocation getModelResource(VolcanicSerpentHeadEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "geo/volcanic_serpent.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(VolcanicSerpentHeadEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "textures/entity/volcanic_serpent.png");
    }

    @Override
    public ResourceLocation getAnimationResource(VolcanicSerpentHeadEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "animations/volcanic_serpent.animation.json");
    }
}
