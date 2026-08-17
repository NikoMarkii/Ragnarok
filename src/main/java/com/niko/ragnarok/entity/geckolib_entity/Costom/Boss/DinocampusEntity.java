package com.niko.ragnarok.entity.geckolib_entity.Costom.Boss;

import com.niko.ragnarok.client.gui.bossbar.ICustomBossBar;
import com.niko.ragnarok.entity.Boss_Monster;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;

public class DinocampusEntity extends Boss_Monster implements GeoEntity, ICustomBossBar {

    protected DinocampusEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    public ResourceLocation getBossBarBaseTexture() {
        return null;
    }

    @Override
    public ResourceLocation getBossBarOverlayTexture() {
        return null;
    }

    @Override
    public float getBossProgress() {
        return 0;
    }

    @Override
    public int getFrameWidth() {
        return 0;
    }

    @Override
    public int getFrameHeight() {
        return 0;
    }

    @Override
    public int getFrameOffsetX() {
        return 0;
    }

    @Override
    public int getFrameOffsetY() {
        return 0;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return null;
    }
}
