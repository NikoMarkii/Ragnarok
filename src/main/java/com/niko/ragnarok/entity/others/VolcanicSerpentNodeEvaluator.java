package com.niko.ragnarok.entity.others;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.Target;

import javax.annotation.Nullable;

public class VolcanicSerpentNodeEvaluator extends NodeEvaluator {

    @Override
    public void prepare(PathNavigationRegion level, Mob mob) {
        super.prepare(level, mob);
    }

    @Override
    public Node getStart() {
        int startX = Mth.floor(this.mob.getX());
        int startY = Mth.floor(this.mob.getY() + 0.5D);
        int startZ = Mth.floor(this.mob.getZ());

        if (this.getBlockPathType(this.level, startX, startY, startZ, this.mob) != BlockPathTypes.BLOCKED) {
            return this.getNode(startX, startY, startZ);
        }

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (this.getBlockPathType(this.level, startX + dx, startY + dy, startZ + dz, this.mob)
                            != BlockPathTypes.BLOCKED) {
                        return this.getNode(startX + dx, startY + dy, startZ + dz);
                    }
                }
            }
        }

        return this.getNode(startX, startY, startZ);
    }

    @Override
    public Target getGoal(double x, double y, double z) {
        return this.getTargetFromNode(this.getNode(Mth.floor(x), Mth.floor(y), Mth.floor(z)));
    }

    @Override
    public int getNeighbors(Node[] outputArray, Node node) {
        int count = 0;

        for (Direction direction : Direction.values()) {
            Node neighbor = this.findFluidNode(
                    node.x + direction.getStepX(),
                    node.y + direction.getStepY(),
                    node.z + direction.getStepZ()
            );
            if (neighbor != null && !neighbor.closed) {
                outputArray[count++] = neighbor;
            }
        }

        return count;
    }

    @Nullable
    private Node findFluidNode(int x, int y, int z) {
        BlockPathTypes pathType = this.getBlockPathType(this.level, x, y, z, this.mob);
        if (pathType == BlockPathTypes.BLOCKED) {
            return null;
        }

        float malus = this.mob.getPathfindingMalus(pathType);
        if (malus < 0.0F) {
            return null;
        }

        Node node = this.getNode(x, y, z);
        node.type = pathType;
        node.costMalus = Math.max(node.costMalus, malus);
        return node;
    }

    @Override
    public BlockPathTypes getBlockPathType(BlockGetter level, int x, int y, int z, Mob mob) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean hasLiquid = false;

        // 浅いマグマ・水中でも詰まらないよう中心基準の2ブロック高で判定
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    pos.set(x + dx, y + dy, z + dz);
                    BlockState state = level.getBlockState(pos);
                    FluidState fluid = state.getFluidState();

                    if (fluid.is(FluidTags.LAVA) || fluid.is(FluidTags.WATER)) {
                        if (dx == 0 && dz == 0) {
                            hasLiquid = true;
                        }
                    } else if (state.blocksMotion() && dx == 0 && dz == 0) {
                        return BlockPathTypes.BLOCKED;
                    }
                }
            }
        }

        if (hasLiquid) {
            return BlockPathTypes.WATER;
        }

        // 液面直上1～2ブロックのみ浮上先(BREACH)として通行可
        for (int down = 1; down <= 2; down++) {
            pos.set(x, y - down, z);
            FluidState belowFluid = level.getFluidState(pos);
            if (belowFluid.is(FluidTags.LAVA) || belowFluid.is(FluidTags.WATER)) {
                return BlockPathTypes.BREACH;
            }
        }

        return BlockPathTypes.BLOCKED;
    }

    @Override
    public BlockPathTypes getBlockPathType(BlockGetter level, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        FluidState fluid = level.getFluidState(pos);
        if (fluid.is(FluidTags.LAVA) || fluid.is(FluidTags.WATER)) {
            return BlockPathTypes.WATER;
        }
        return level.getBlockState(pos).blocksMotion()
                ? BlockPathTypes.BLOCKED
                : BlockPathTypes.OPEN;
    }
}