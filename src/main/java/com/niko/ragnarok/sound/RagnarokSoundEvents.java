package com.niko.ragnarok.sound;

import com.niko.ragnarok.Ragnarok;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class RagnarokSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Ragnarok.MOD_ID);

    public static final RegistryObject<SoundEvent> TLEX_AMBIENT =
            register("entity.tlex.ambient");

    public static final RegistryObject<SoundEvent> TLEX_STEP =
            register("entity.tlex.step");

    public static final RegistryObject<SoundEvent> TLEX_ROAR =
            register("entity.tlex.roar");




    private static RegistryObject<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name,
                () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Ragnarok.MOD_ID, name)));
    }
}
