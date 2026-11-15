package com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.VolcanicSerpentEntity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class VolcanicSerpentBodyEntity extends VolcanicSerpentPartEntity {
    public VolcanicSerpentBodyEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createPartAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0D)
                .add(Attributes.ARMOR, 30.0D);
    }
}
