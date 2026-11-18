package com.niko.ragnarok.entity.others;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.Vec3;

public class VolcanicSerpentPathNavigation extends PathNavigation {

    public VolcanicSerpentPathNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new VolcanicSerpentNodeEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
    }

    @Override
    protected boolean canUpdatePath() {
        return this.mob.isInWater() || this.mob.isInLava() || this.mob.onGround();
    }

    @Override
    protected Vec3 getTempMobPos() {
        return new Vec3(this.mob.getX(), this.mob.getY(0.5D), this.mob.getZ());
    }

    @Override
    protected double getGroundY(Vec3 pos) {
        return pos.y;
    }

    @Override
    protected boolean canMoveDirectly(Vec3 start, Vec3 end) {
        return isClearForMovementBetween(this.mob, start, end, false);
    }

    @Override
    public boolean isStableDestination(BlockPos pos) {
        FluidState fluid = this.level.getFluidState(pos);
        if (fluid.is(FluidTags.LAVA) || fluid.is(FluidTags.WATER)) {
            return true;
        }
        return !this.level.getBlockState(pos).isAir();
    }

    @Override
    protected void followThePath() {
        Vec3 mobPos = this.getTempMobPos();
        // 大型サーペントの旋回半径より大きい到達距離を確保し、同じ地点での周回を防ぐ
        this.maxDistanceToWaypoint = Math.max(2.25F, this.mob.getBbWidth() * 0.9F);

        if (this.path != null && !this.path.isDone()) {
            Vec3 targetPos = this.path.getNextEntityPos(this.mob);
            double dx = Math.abs(this.mob.getX() - targetPos.x);
            double dy = Math.abs(this.mob.getY() - targetPos.y);
            double dz = Math.abs(this.mob.getZ() - targetPos.z);

            if (dx < this.maxDistanceToWaypoint
                    && dz < this.maxDistanceToWaypoint
                    && dy < this.maxDistanceToWaypoint) {
                this.path.advance();
            } else if (this.path.getNextNodeIndex() + 1 < this.path.getNodeCount()) {
                // 次のノードのほうが近い場合も先へ進める
                Vec3 nextPos = this.path.getEntityPosAtNode(this.mob, this.path.getNextNodeIndex() + 1);
                if (this.mob.distanceToSqr(nextPos) < this.mob.distanceToSqr(targetPos)) {
                    this.path.advance();
                }
            }
        }

        this.doStuckDetection(mobPos);
    }
}