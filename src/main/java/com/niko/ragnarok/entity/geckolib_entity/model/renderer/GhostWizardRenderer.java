package com.niko.ragnarok.entity.geckolib_entity.model.renderer;

import com.niko.ragnarok.entity.geckolib_entity.Costom.GhostWizardEntity;
import com.niko.ragnarok.entity.geckolib_entity.model.GhostWizardModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EntityType;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GhostWizardRenderer extends GeoEntityRenderer<GhostWizardEntity> {

    public GhostWizardRenderer(EntityRendererProvider.Context context) {
        super(context, new GhostWizardModel());
        this.shadowRadius = 0.5F;
    }
}
