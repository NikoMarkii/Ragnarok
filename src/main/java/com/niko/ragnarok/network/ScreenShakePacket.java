package com.niko.ragnarok.network;

import com.niko.ragnarok.client.ScreenShakeHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ScreenShakePacket {

    private final float intensity; // 揺れの強さ
    private final int duration;    // 揺れの長さ（tick）

    public ScreenShakePacket(float intensity, int duration) {
        this.intensity = intensity;
        this.duration = duration;
    }

    public static ScreenShakePacket decode(FriendlyByteBuf buf) {
        return new ScreenShakePacket(buf.readFloat(), buf.readInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeFloat(intensity);
        buf.writeInt(duration);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                ScreenShakeHandler.startShake(intensity, duration));
        ctx.get().setPacketHandled(true);
    }
}
