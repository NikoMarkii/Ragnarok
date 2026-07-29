package com.niko.ragnarok.block;

import com.niko.ragnarok.blockentities.DoorsofkingroomBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class DoorDummyBlock extends Block {

    public DoorDummyBlock(Properties properties) {
        super(properties);
    }

    // 見た目は透明（GeckoLib側が一括描画するためダミー自体は描画しない）
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    // ダミーが右クリックされたら、親（メインブロック）の右クリック処理を呼び出す
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hitResult) {
        // 親（メイン）の座標を探索（例：下方向や周囲を探索、またはNBT等で特定）
        BlockPos parentPos = findParentPos(level, pos);
        if (parentPos != null) {
            BlockState parentState = level.getBlockState(parentPos);
            if (parentState.getBlock() instanceof DoorsofKingroomBlock parentBlock) {
                return parentBlock.use(parentState, level, parentPos, player, hand, hitResult);
            }
        }
        return InteractionResult.PASS;
    }

    // ダミーが破壊されたら、親（メインブロック）も一緒に破壊する
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockPos parentPos = findParentPos(level, pos);
        if (parentPos != null && !level.isClientSide) {
            level.destroyBlock(parentPos, true); // 親を破壊（ドロップあり）
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    // 近くにあるメインブロックの位置を探す補助メソッド
    // （幅3×高さ5の範囲内を下に手繰ってメインブロックを探す）
    private BlockPos findParentPos(Level level, BlockPos currentPos) {
        for (int y = 0; y < 5; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos checkPos = currentPos.offset(x, -y, z);
                    if (level.getBlockState(checkPos).getBlock() instanceof DoorsofKingroomBlock) {
                        return checkPos;
                    }
                }
            }
        }
        return null;
    }
}