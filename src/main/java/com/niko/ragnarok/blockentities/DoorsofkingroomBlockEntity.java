package com.niko.ragnarok.blockentities;

import com.niko.ragnarok.block.DoorDummyBlock;
import com.niko.ragnarok.block.DoorsofKingroomBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

public class DoorsofkingroomBlockEntity extends BlockEntity implements GeoBlockEntity {
    private boolean isOpen = false;
    private boolean isOpening = false;
    private int openTimer = 0;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public DoorsofkingroomBlockEntity(BlockPos pos, BlockState state) {
        super(RagnarokBlockEntities.DOORS_KING_ROOM.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("IsOpen", this.isOpen);
        tag.putBoolean("IsOpening", this.isOpening);
        tag.putInt("OpenTimer", this.openTimer);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.isOpen = tag.getBoolean("IsOpen");
        this.isOpening = tag.getBoolean("IsOpening");
        this.openTimer = tag.getInt("OpenTimer");
    }

    // --- セーブロード・パケット受信時の同期処理 ---
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // サーバーからパケットを受け取った際にクライアント側のNBTを更新する
    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            load(tag);
        }
    }

    public void open() {
        if (this.isOpen || this.isOpening) return;
        this.isOpening = true;
        this.openTimer = 80; // 4秒 = 80 ticks
        this.setChanged();

        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;

        if (this.isOpening) {
            this.openTimer--;
            if (this.openTimer <= 0) {
                this.isOpening = false;
                this.isOpen = true;
                this.setChanged();

                level.setBlock(pos, state.setValue(DoorsofKingroomBlock.OPEN, true), 3);
                level.sendBlockUpdated(pos, state, state, 3);

                // 4秒経過後、ダミーブロックを消去して通り抜け可能にする
                Direction facing = state.getValue(DoorsofKingroomBlock.FACING);
                Direction side = facing.getClockWise();

                for (int h = 0; h < 5; h++) {
                    for (int w = -1; w <= 1; w++) {
                        if (h == 0 && w == 0) continue;
                        BlockPos targetPos = pos.relative(side, w).above(h);
                        if (level.getBlockState(targetPos).getBlock() instanceof DoorDummyBlock) {
                            level.removeBlock(targetPos, false);
                        }
                    }
                }
            }
        }
    }

    public boolean isOpen() {
        return this.isOpen;
    }

    public boolean isOpening() {
        return this.isOpening;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(this.worldPosition).inflate(3.0, 5.0, 3.0);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<DoorsofkingroomBlockEntity> event) {
        // 開閉アニメーション中、または既に開ききっている（ロード後含む）場合
        if (this.isOpening() || this.isOpen()) {
            event.getController().setAnimation(RawAnimation.begin().thenPlayAndHold("open"));
            return PlayState.CONTINUE;
        }

        // 閉じ状態
        return PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}