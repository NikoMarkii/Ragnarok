package com.niko.ragnarok.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.trading.Merchant;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 会話中の商人モブをプレイヤーに紐付け、会話終了時に解除するパケット。
 * Merchant を実装する任意のエンティティで利用できる。
 */
public class DialogueStatePacket {
    private final int entityId;
    private final boolean active;

    public DialogueStatePacket(int entityId, boolean active) {
        this.entityId = entityId;
        this.active = active;
    }

    public static void encode(DialogueStatePacket message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.entityId);
        buffer.writeBoolean(message.active);
    }

    public static DialogueStatePacket decode(FriendlyByteBuf buffer) {
        return new DialogueStatePacket(buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(DialogueStatePacket message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            Entity entity = player == null ? null : player.level().getEntity(message.entityId);
            if (player == null || !(entity instanceof Merchant merchant)
                    || player.distanceToSqr(entity) > 64.0D) {
                return;
            }

            if (message.active) {
                merchant.setTradingPlayer(player);
            } else if (merchant.getTradingPlayer() == player) {
                merchant.setTradingPlayer(null);
            }
        });
        context.setPacketHandled(true);
    }
}
