package com.niko.ragnarok.entity.geckolib_entity.model.renderer;

import com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.Volcanic_Serpent.VolcanicSerpentPartEntity;
import com.niko.ragnarok.entity.geckolib_entity.model.VolcanicSerpentPartModel;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class VolcanicSerpentPartRenderer
        extends GeoEntityRenderer<VolcanicSerpentPartEntity> {

    public VolcanicSerpentPartRenderer(EntityRendererProvider.Context context) {
        super(context, new VolcanicSerpentPartModel());
        this.shadowRadius = 1.5F;
    }
}
