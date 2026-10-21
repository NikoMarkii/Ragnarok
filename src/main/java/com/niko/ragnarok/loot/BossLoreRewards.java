package com.niko.ragnarok.loot;

import com.niko.ragnarok.entity.Boss_Monster;
import com.niko.ragnarok.world.BossLoreData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

public final class BossLoreRewards {
    private static final String PARTICIPANTS_KEY = "ragnarok:lore_participants";
    private static final long PARTICIPATION_TIMEOUT = 20L * 60L * 5L;

    private BossLoreRewards() {
    }

    public static void recordHit(Boss_Monster boss, DamageSource source) {
        if (!(boss.level() instanceof ServerLevel level)) {
            return;
        }
        Entity attacker = source.getEntity();
        if (attacker instanceof OwnableEntity owned) {
            attacker = owned.getOwner();
        }
        if (attacker instanceof ServerPlayer player && !player.isSpectator()) {
            CompoundTag participants = boss.getPersistentData().getCompound(PARTICIPANTS_KEY);
            participants.putLong(player.getUUID().toString(), level.getGameTime());
            boss.getPersistentData().put(PARTICIPANTS_KEY, participants);
        }
    }

    public static void award(Boss_Monster boss, DamageSource source, Item loreItem) {
        if (!(boss.level() instanceof ServerLevel level)) {
            return;
        }
        ResourceLocation bossId = ForgeRegistries.ENTITY_TYPES.getKey(boss.getType());
        if (bossId == null) {
            return;
        }

        recordHit(boss, source);
        CompoundTag participants = boss.getPersistentData().getCompound(PARTICIPANTS_KEY);
        BossLoreData data = BossLoreData.get(level.getServer());
        for (String playerKey : participants.getAllKeys()) {
            long elapsed = level.getGameTime() - participants.getLong(playerKey);
            if (elapsed < 0 || elapsed > PARTICIPATION_TIMEOUT) {
                continue;
            }
            UUID playerId;
            try {
                playerId = UUID.fromString(playerKey);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            data.queueReward(playerId, bossId, loreItem);
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            if (player != null) {
                data.deliverPending(player);
            }
        }
        boss.getPersistentData().remove(PARTICIPANTS_KEY);
    }
}
