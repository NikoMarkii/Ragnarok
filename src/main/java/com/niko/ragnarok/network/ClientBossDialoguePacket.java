package com.niko.ragnarok.network;

import com.niko.ragnarok.client.gui.BossDialogueOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class ClientBossDialoguePacket {
    private final String text;
    private final ResourceLocation icon;
    private final ResourceLocation soundLocation;

    public ClientBossDialoguePacket(String text, ResourceLocation icon, ResourceLocation soundLocation) {
        this.text = text;
        this.icon = icon;
        this.soundLocation = soundLocation;
    }

    public static void encode(ClientBossDialoguePacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.text);
        buf.writeResourceLocation(msg.icon);
        buf.writeResourceLocation(msg.soundLocation);
    }

    public static ClientBossDialoguePacket decode(FriendlyByteBuf buf) {
        String text = buf.readUtf();
        ResourceLocation icon = buf.readResourceLocation();
        ResourceLocation sound = buf.readResourceLocation();
        return new ClientBossDialoguePacket(text, icon, sound);
    }

    public static void handle(ClientBossDialoguePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(msg.soundLocation);
            // 指定された顔グラ（icon）を渡して表示呼び出し！
            BossDialogueOverlay.show(msg.text, sound, msg.icon);
        });
        ctx.get().setPacketHandled(true);
    }
}