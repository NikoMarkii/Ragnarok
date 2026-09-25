package com.niko.ragnarok.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

public class WorldModeData extends SavedData {
    private static final String DATA_NAME = "ragnarok_world_mode";

    public enum GameModeState {
        NORMAL(0),
        HARD(1),
        MASTER(2);

        private final int id;

        GameModeState(int id) {
            this.id = id;
        }

        public int getId() { return id; }

        public static GameModeState fromId(int id) {
            for (GameModeState state : values()) {
                if (state.getId() == id) return state;
            }
            return NORMAL;
        }
    }

    private GameModeState currentState = GameModeState.NORMAL;

    public GameModeState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(GameModeState state) {
        this.currentState = state;
        this.setDirty(); // 変更をNBTへ保存フラグ立て
    }

    public static WorldModeData get(ServerLevel level) {
        // オーバーワールドのデータストレージから一括管理
        DimensionDataStorage storage = level.getServer().overworld().getDataStorage();
        return storage.computeIfAbsent(WorldModeData::load, WorldModeData::new, DATA_NAME);
    }

    public static WorldModeData load(CompoundTag tag) {
        WorldModeData data = new WorldModeData();
        data.currentState = GameModeState.fromId(tag.getInt("GameModeState"));
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("GameModeState", this.currentState.getId());
        return tag;
    }
}
