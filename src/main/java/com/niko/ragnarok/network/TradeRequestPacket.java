package com.niko.ragnarok.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.trading.Merchant;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Merchant を実装する任意のモブの取引画面をサーバー側で開くパケット。 */
public class TradeRequestPacket {
    private final int entityId;

    public TradeRequestPacket(int entityId) {
        this.entityId = entityId;
    }

    public static void encode(TradeRequestPacket message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.entityId);
    }

    public static TradeRequestPacket decode(FriendlyByteBuf buffer) {
        return new TradeRequestPacket(buffer.readVarInt());
    }

    public static void handle(TradeRequestPacket message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            Entity entity = player == null ? null : player.level().getEntity(message.entityId);
            if (player == null || !(entity instanceof Merchant merchant)
                    || player.distanceToSqr(entity) > 64.0D) {
                return;
            }

            merchant.setTradingPlayer(player);
            merchant.openTradingScreen(player, entity.getDisplayName(), 1);
        });
        context.setPacketHandled(true);
    }
}
