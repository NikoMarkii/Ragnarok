package com.niko.ragnarok.entity.geckolib_entity.model.renderer;

import com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.VolcanicSerpentEntity.VolcanicSerpentTailEntity;
import com.niko.ragnarok.entity.geckolib_entity.model.VolcanicSerpentTailModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class VolcanicSerpentTailRenderer extends GeoEntityRenderer<VolcanicSerpentTailEntity> {
    public VolcanicSerpentTailRenderer(EntityRendererProvider.Context context) {
        super(context, new VolcanicSerpentTailModel());
        this.shadowRadius = 2.0F;
    }
}
