package com.niko.ragnarok.client.gui;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import javax.annotation.Nullable;

public class BossDialogueData {
    private final String text;
    private final ResourceLocation icon;
    @Nullable
    private final SoundEvent sound;

    public BossDialogueData(String text, ResourceLocation icon, @Nullable SoundEvent sound) {
        this.text = text;
        this.icon = icon;
        this.sound = sound;
    }

    public String getText() { return text; }
    public ResourceLocation getIcon() { return icon; }
    @Nullable
    public SoundEvent getSound() { return sound; }
}