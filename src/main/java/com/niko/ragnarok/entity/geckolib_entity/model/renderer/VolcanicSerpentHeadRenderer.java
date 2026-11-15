package com.niko.ragnarok.entity.geckolib_entity.model.renderer;

import com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.VolcanicSerpentEntity.VolcanicSerpentHeadEntity;
import com.niko.ragnarok.entity.geckolib_entity.model.VolcanicSerpentHeadModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class VolcanicSerpentHeadRenderer extends GeoEntityRenderer<VolcanicSerpentHeadEntity> {
    public VolcanicSerpentHeadRenderer(EntityRendererProvider.Context context) {
        super(context, new VolcanicSerpentHeadModel());
        this.shadowRadius = 3.0F;
    }
}
