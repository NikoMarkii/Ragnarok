package com.niko.ragnarok.entity;

import com.niko.ragnarok.Ragnarok;
import com.niko.ragnarok.entity.Projectile.BlueFireballEntity;
import com.niko.ragnarok.entity.Projectile.GlowingDustEntity;
import com.niko.ragnarok.entity.Projectile.VoidSlashEntity;
import com.niko.ragnarok.entity.costom.*;
import com.niko.ragnarok.entity.geckolib_entity.Costom.*;
import com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.GradiusEntity;
import com.niko.ragnarok.entity.geckolib_entity.Costom.Fairy.FairyEntity;
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
    public static final RegistryObject<EntityType<EnderSoldierEntity>> ENDER_SOLDIER =
            ENTITY_TYPES.register("ender_soldier",() ->
                    EntityType.Builder.of(EnderSoldierEntity::new,MobCategory.MONSTER)
                            .sized(3F,4.5F)
                            .build("ender_soldier")
            );
    public static final RegistryObject<EntityType<FairyEntity>> FAIRY = ENTITY_TYPES.register("fairy",
            () -> EntityType.Builder.of(FairyEntity::new, MobCategory.CREATURE)
                    .sized(1F, 0.9F) // 小さなサイズ
                    .clientTrackingRange(8)
                    .build("fairy"));

    public static final RegistryObject<EntityType<CassowaryEntity>> CASSOWARY =
            ENTITY_TYPES.register("cassowary", () ->
                    EntityType.Builder.of(CassowaryEntity::new, MobCategory.CREATURE)
                            .sized(1.2F, 2.1F)
                            .clientTrackingRange(8)
                            .build("cassowary"));

    public static final RegistryObject<EntityType<GradiusEntity>> GRADIUS =
            ENTITY_TYPES.register("gradius",() ->
                    EntityType.Builder.of(GradiusEntity::new, MobCategory.MONSTER)
                            .sized(2.5F,3.6F)
                            .build("gradius"));

    public static final RegistryObject<EntityType<GaleosEntity>> GALEOS =
            ENTITY_TYPES.register("galeos", () ->
                    EntityType.Builder.<GaleosEntity>of(GaleosEntity::new, MobCategory.MONSTER)
                            .sized(3.5F, 6.5F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("galeos")
            );

    public static final RegistryObject<EntityType<VoidSlashEntity>> VOID_SLASH =
            ENTITY_TYPES.register("void_slash", () ->
                    EntityType.Builder.<VoidSlashEntity>of(VoidSlashEntity::new, MobCategory.MISC) // 飛び道具は通常 MISC
                            .sized(4F, 1F) // 当たり判定の大きさ。斬撃なら小さめでOK
                            .clientTrackingRange(4) // どのくらいの距離から表示を開始するか
                            .updateInterval(10) // 位置同期の頻度（小さいほど滑らか）
                            .build("void_slash")
            );
    public static final RegistryObject<EntityType<GlowingDustEntity>> GLOWING_DUST_PROJECTILE =
            ENTITY_TYPES.register("glowing_dust_entity", () ->
                    EntityType.Builder.<GlowingDustEntity>of(GlowingDustEntity::new, MobCategory.MISC) // 飛び道具は通常 MISC
                            .sized(1F, 1F) // 当たり判定の大きさ。斬撃なら小さめでOK
                            .clientTrackingRange(7) // どのくらいの距離から表示を開始するか
                            .updateInterval(10) // 位置同期の頻度（小さいほど滑らか）
                            .build("glowing_dust_entity")
            );
    public static final RegistryObject<EntityType<BlueFireballEntity>> BLUE_FIREBALL =
            ENTITY_TYPES.register("blue_fireball", () ->
                    EntityType.Builder.<BlueFireballEntity>of(BlueFireballEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)   // 1Fは大きすぎ、ブロックに引っかかる原因にも
                            .clientTrackingRange(64) // 発射物は広めに（デフォルト64）
                            .updateInterval(1)   // 毎tick同期（発射物は必ず1にする）
                            .build("blue_fireball")
            );
    public static final RegistryObject<EntityType<GhostEntity>> GHOST =
            ENTITY_TYPES.register("ghost", () ->
                    EntityType.Builder.of(GhostEntity::new, MobCategory.MONSTER)
                            .sized(0.8F, 1.8F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("ghost")
            );
    public static final RegistryObject<EntityType<GhostKnightEntity>> GHOST_KNIGHT =
            ENTITY_TYPES.register("ghost_knight", () ->
                    EntityType.Builder.<GhostKnightEntity>of(GhostKnightEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("ghost_knight")
            );
    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }

}
