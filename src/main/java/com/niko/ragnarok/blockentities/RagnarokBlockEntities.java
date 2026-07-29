package com.niko.ragnarok.blockentities;

import com.niko.ragnarok.block.RagnarokBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class RagnarokBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"ragnarok");
public static final Supplier<BlockEntityType<DoorsofkingroomBlockEntity>> DOORS_KING_ROOM =
        BLOCK_ENTITIES.register("doors_king_room", () ->
                BlockEntityType.Builder.of(
                        DoorsofkingroomBlockEntity::new,
                        RagnarokBlocks.DOORS_KING_ROOM.get()
                ).build(null)
        );
}
