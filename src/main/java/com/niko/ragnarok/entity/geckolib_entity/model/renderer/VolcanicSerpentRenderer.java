package com.niko.ragnarok.entity.geckolib_entity.model.renderer;


import com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.Volcanic_Serpent.VolcanicSerpentEntity;
import com.niko.ragnarok.entity.geckolib_entity.model.VolcanicSerpentHeadModel;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class VolcanicSerpentRenderer
        extends GeoEntityRenderer<VolcanicSerpentEntity> {

    public VolcanicSerpentRenderer(EntityRendererProvider.Context context) {
        super(context, new VolcanicSerpentHeadModel());
        this.shadowRadius = 2.0F;
    }
}