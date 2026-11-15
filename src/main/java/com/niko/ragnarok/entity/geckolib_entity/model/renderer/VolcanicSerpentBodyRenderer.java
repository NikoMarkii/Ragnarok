package com.niko.ragnarok.entity.geckolib_entity.model.renderer;

import com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.VolcanicSerpentEntity.VolcanicSerpentBodyEntity;
import com.niko.ragnarok.entity.geckolib_entity.model.VolcanicSerpentBodyModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class VolcanicSerpentBodyRenderer extends GeoEntityRenderer<VolcanicSerpentBodyEntity> {
    public VolcanicSerpentBodyRenderer(EntityRendererProvider.Context context) {
        super(context, new VolcanicSerpentBodyModel());
        this.shadowRadius = 2.0F;
    }
}
