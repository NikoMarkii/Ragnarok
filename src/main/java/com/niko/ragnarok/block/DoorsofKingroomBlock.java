package com.niko.ragnarok.block;

import com.niko.ragnarok.blockentities.DoorsofkingroomBlockEntity;
import com.niko.ragnarok.blockentities.RagnarokBlockEntities;
import com.niko.ragnarok.item.Ragnarok_mainItems;
import com.niko.ragnarok.sound.RagnarokSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class DoorsofKingroomBlock extends Block implements EntityBlock {

    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    // 1. 向き（水平4方向）を保持するプロパティを追加
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public DoorsofKingroomBlock(Properties properties) {
        super(properties);
        // デフォルト状態に FACING (デフォルト NORTH) を設定
        this.registerDefaultState(this.stateDefinition.any().setValue(OPEN, false).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN, FACING);
    }

    // 2. プレイヤーがブロックを設置した時の向き（プレイヤーと対面する方向）を取得
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    // 3. 向き（FACING）に応じた 幅3 × 高さ5 × 奥行き1（16px）のVoxelShapeを生成
    private VoxelShape getShapeForFacing(Direction facing) {
        VoxelShape shape = Shapes.empty();

        if (facing == Direction.NORTH || facing == Direction.SOUTH) {
            // NORTH / SOUTH の場合: X軸方向に幅3（-1〜+1）、Z軸方向に奥行き1（0〜16px）、Y軸方向に高さ5（0〜5）
            for (int x = -1; x <= 1; x++) {
                for (int y = 0; y < 5; y++) {
                    shape = Shapes.or(shape, Block.box(x * 16.0, y * 16.0, 0.0, (x + 1) * 16.0, (y + 1) * 16.0, 16.0));
                }
            }
        } else {
            // EAST / WEST の場合: Z軸方向に幅3（-1〜+1）、X軸方向に奥行き1（0〜16px）、Y軸方向に高さ5（0〜5）
            for (int z = -1; z <= 1; z++) {
                for (int y = 0; y < 5; y++) {
                    shape = Shapes.or(shape, Block.box(0.0, y * 16.0, z * 16.0, 16.0, (y + 1) * 16.0, (z + 1) * 16.0));
                }
            }
        }

        return shape;
    }
    // 設置可能かどうかの判定（3×5の領域がすべて空気または置換可能か）
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        Direction side = facing.getClockWise();

        for (int h = 0; h < 5; h++) {
            for (int w = -1; w <= 1; w++) {
                if (h == 0 && w == 0) continue;
                BlockPos targetPos = pos.relative(side, w).above(h);
                BlockState targetState = level.getBlockState(targetPos);
                if (!targetState.canBeReplaced()) {
                    return false; // 障害物がある場合は設置不可
                }
            }
        }
        return true;
    }

    // 設置完了時に周囲をダミーブロックで埋める
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            Direction facing = state.getValue(FACING);
            Direction side = facing.getClockWise();

            for (int h = 0; h < 5; h++) {
                for (int w = -1; w <= 1; w++) {
                    if (h == 0 && w == 0) continue;
                    BlockPos targetPos = pos.relative(side, w).above(h);
                    // ダミーブロック（ModBlocks.DOOR_DUMMY_BLOCK）を設置
                    level.setBlock(targetPos, RagnarokBlocks.DOOR_DUMMY_BLOCK.get().defaultBlockState(), 3);
                }
            }
        }
    }

    // メイン本体が破壊されたら、周囲のダミーブロックを一括削除
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide) {
                Direction facing = state.getValue(FACING);
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
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DoorsofkingroomBlockEntity(pos, state);
    }

    // --- 選択・破壊用判定：現在の向きに合わせたVoxelShapeを返す ---
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShapeForFacing(state.getValue(FACING));
    }

    // --- 衝突（移動制限）判定 ---
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPEN) ? Shapes.empty() : getShapeForFacing(state.getValue(FACING));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (state.getValue(OPEN)) {
            return InteractionResult.SUCCESS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (hand == InteractionHand.MAIN_HAND) {
            ItemStack heldItem = player.getItemInHand(hand);

            if (heldItem.is(Ragnarok_mainItems.KING_KEY.get())) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof DoorsofkingroomBlockEntity doorBE) {
                    if (doorBE.isOpening() || doorBE.isOpen()) {
                        return InteractionResult.SUCCESS;
                    }

                    doorBE.open();
                    level.playSound(null, pos, RagnarokSoundEvents.OPEN_DOORS_KING_ROOM.get(), SoundSource.BLOCKS, 1.0F, 0.7F);
                    return InteractionResult.CONSUME;
                }
            }

            level.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0F, 1.0F);
            return InteractionResult.FAIL;
        }

        return InteractionResult.PASS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == RagnarokBlockEntities.DOORS_KING_ROOM.get()) {
            return (lvl, pos, st, be) -> {
                if (be instanceof DoorsofkingroomBlockEntity doorBE) {
                    doorBE.tick(lvl, pos, st);
                }
            };
        }
        return null;
    }
}