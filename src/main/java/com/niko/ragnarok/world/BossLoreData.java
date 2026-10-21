package com.niko.ragnarok.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.UUID;

public class BossLoreData extends SavedData {
    private static final String DATA_NAME = "ragnarok_boss_lore";
    private CompoundTag unlocked = new CompoundTag();
    private CompoundTag pending = new CompoundTag();

    public static BossLoreData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(BossLoreData::load, BossLoreData::new, DATA_NAME);
    }

    public static BossLoreData load(CompoundTag tag) {
        BossLoreData data = new BossLoreData();
        data.unlocked = tag.getCompound("Unlocked");
        data.pending = tag.getCompound("Pending");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.put("Unlocked", unlocked);
        tag.put("Pending", pending);
        return tag;
    }

    public void queueReward(UUID playerId, ResourceLocation bossId, Item loreItem) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(loreItem);
        if (itemId == null || loreItem == Items.AIR) {
            return;
        }

        String playerKey = playerId.toString();
        String bossKey = bossId.toString();
        CompoundTag playerUnlocked = unlocked.getCompound(playerKey);
        if (playerUnlocked.getBoolean(bossKey)) {
            return;
        }

        playerUnlocked.putBoolean(bossKey, true);
        unlocked.put(playerKey, playerUnlocked);
        CompoundTag playerPending = pending.getCompound(playerKey);
        playerPending.putString(bossKey, itemId.toString());
        pending.put(playerKey, playerPending);
        setDirty();
    }

    public void deliverPending(ServerPlayer player) {
        String playerKey = player.getUUID().toString();
        if (!player.isAlive() || player.isSpectator() || !pending.contains(playerKey)) {
            return;
        }

        CompoundTag playerPending = pending.getCompound(playerKey);
        for (String bossKey : new ArrayList<>(playerPending.getAllKeys())) {
            if (player.getInventory().getFreeSlot() < 0) {
                break;
            }
            ResourceLocation itemId = ResourceLocation.tryParse(playerPending.getString(bossKey));
            Item item = itemId == null ? null : ForgeRegistries.ITEMS.getValue(itemId);
            if (item == null || item == Items.AIR) {
                continue;
            }

            ItemStack reward = new ItemStack(item);
            if (player.getInventory().add(reward)) {
                playerPending.remove(bossKey);
                setDirty();
                player.containerMenu.broadcastChanges();
                player.sendSystemMessage(Component.translatable("message.ragnarok.boss_lore.received", item.getDescription()));
            }
        }
        if (playerPending.isEmpty()) {
            pending.remove(playerKey);
        }
    }
}
