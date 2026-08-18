package com.niko.ragnarok.entity.geckolib_entity.model;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.Projectile.DinocampusBubbleEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DinocampusBubbleModel extends GeoModel<DinocampusBubbleEntity> {
    @Override
    public ResourceLocation getModelResource(DinocampusBubbleEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "geo/dinocampus_bubble_1.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DinocampusBubbleEntity entity) {
        String texture = switch (entity.getVariant()) {
            case DinocampusBubbleEntity.RED -> "textures/entity/dinocampus_bubble_2.png";
            case DinocampusBubbleEntity.YELLOW -> "textures/entity/dinocampus_bubble_3.png";
            default -> "textures/entity/dinocampus_bubble_1.png";
        };
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, texture);
    }

    @Override
    public ResourceLocation getAnimationResource(DinocampusBubbleEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, "animations/bubble_1.animation.json");
    }
}
