package com.niko.ragnarok.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * サーバー側のボス（GradiusEntity等）から、特定のプレイヤーへ
 * ボス戦中用のテキストボックス表示を指示するパケット。
 *
 * Componentのまま送ることで、翻訳キー(message.ragnarok.gradius.death 等)を
 * 受信側（クライアント）のロケールでちゃんと解決させている
 * （サーバー側で文字列に変換してしまうと、プレイヤーごとの言語設定が反映されない）。
 */
public class ClientBossDialoguePacket {

    private final Component text;
    @Nullable
    private final ResourceLocation typingSoundId;

    public ClientBossDialoguePacket(Component text, @Nullable ResourceLocation typingSoundId) {
        this.text = text;
        this.typingSoundId = typingSoundId;
    }

    public static void encode(ClientBossDialoguePacket msg, FriendlyByteBuf buf) {
        buf.writeComponent(msg.text);
        buf.writeBoolean(msg.typingSoundId != null);
        if (msg.typingSoundId != null) {
            buf.writeResourceLocation(msg.typingSoundId);
        }
    }

    public static ClientBossDialoguePacket decode(FriendlyByteBuf buf) {
        Component text = buf.readComponent();
        ResourceLocation soundId = buf.readBoolean() ? buf.readResourceLocation() : null;
        return new ClientBossDialoguePacket(text, soundId);
    }

    public static void handle(ClientBossDialoguePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            SoundEvent sound = msg.typingSoundId != null
                    ? ForgeRegistries.SOUND_EVENTS.getValue(msg.typingSoundId)
                    : null;
            // ここで初めて.getString()を呼ぶ＝クライアントのロケールで解決される
            com.niko.ragnarok.client.gui.BossDialogueOverlay.show(msg.text.getString(), sound);
        });
        ctx.setPacketHandled(true);
    }
}