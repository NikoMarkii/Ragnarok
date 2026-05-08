package com.niko.ragnarok.entity;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.costom.*;
import com.niko.ragnarok.entity.geckolib_entity.Costom.Ender_Soldier;
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
            ENTITY_TYPES.register("t_lex",() -> EntityType.Builder.of(TLex::new, MobCategory.CREATURE)
                    .sized(3.0F, 4.0F)
                    .build("t_lex"));

    public static final RegistryObject<EntityType<Groot>> GROOT =
            ENTITY_TYPES.register("groot",() -> EntityType.Builder.of(Groot::new, MobCategory.CREATURE)
                    .sized(3.0F,4.5F)
                    .build("groot"));

    public static final RegistryObject<EntityType<Mini_Groot>> MINI_GROOT =
            ENTITY_TYPES.register("mini_groot", () ->
                    EntityType.Builder.<Mini_Groot>of(Mini_Groot::new, MobCategory.CREATURE)
                            .sized(1F, 1.2F)
                            .build("mini_groot")
            );

    public static final RegistryObject<EntityType<Magic_Golem>> MAGIC_GOLEM =
            ENTITY_TYPES.register("magic_golem",() ->
                    EntityType.Builder.of(Magic_Golem::new, MobCategory.MONSTER)
                            .sized(2F,2.0F)
                            .build("magic_golem")
            );
    public static final RegistryObject<EntityType<Ender_Soldier>> ENDER_SOLDIER =
            ENTITY_TYPES.register("ender_soldier",() ->
                    EntityType.Builder.of(Ender_Soldier::new,MobCategory.MONSTER)
                            .sized(3F,4.5F)
                            .build("ender_soldier")
            );

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }

}
