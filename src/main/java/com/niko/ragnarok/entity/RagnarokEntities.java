package com.niko.ragnarok.entity;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.costom.Groot;
import com.niko.ragnarok.entity.costom.RedCreeper;
import com.niko.ragnarok.entity.costom.Scorpion;
import com.niko.ragnarok.entity.costom.TLex;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class RagnarokEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,
                    Ragnarok.MOD_ID);
    public static final RegistryObject<EntityType<RedCreeper>> RED_CREEPER =
            ENTITY_TYPES.register("red_creeper",() -> EntityType.Builder.of(RedCreeper::new, MobCategory.MONSTER)
                    .build("red_creeper"));
    public static final RegistryObject<EntityType<Scorpion>> SCORPION =
            ENTITY_TYPES.register("scorpion",() -> EntityType.Builder.of(Scorpion::new, MobCategory.MONSTER)
                    .sized(1.7F, 1.7F)
                    .build("scorpion"));

    public static final RegistryObject<EntityType<TLex>> T_LEX =
            ENTITY_TYPES.register("t_lex",() -> EntityType.Builder.of(TLex::new, MobCategory.AMBIENT)
                    .sized(3.0F, 4.0F)
                    .build("t_lex"));

    public static final RegistryObject<EntityType<Groot>> GROOT =
            ENTITY_TYPES.register("groot",() -> EntityType.Builder.of(Groot::new, MobCategory.MONSTER)
                    .sized(3.0F,4.5F)
                    .build("groot"));
    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }

}
