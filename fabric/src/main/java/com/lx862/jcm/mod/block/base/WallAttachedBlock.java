package com.lx862.jcm.mod.block.base;

import com.lx862.jcm.mod.block.behavior.WallAttachedBlockBehavior;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.*;

public abstract class WallAttachedBlock extends DirectionalBlock implements WallAttachedBlockBehavior {
    public WallAttachedBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        BlockState superState = super.getPlacementState2(ctx);
        if (superState == null) return null;

        Direction facing = BlockUtil.getProperty(superState, FACING);
        boolean isAttached = WallAttachedBlockBehavior.canBePlaced(ctx.getBlockPos(), World.cast(ctx.getWorld()), Direction.fromHorizontal(ctx.getSide().getHorizontal()).getOpposite());
        if(!isAttached) return null;

        return superState.with(new Property<>(FACING.data), Direction.fromHorizontal(ctx.getSide().getHorizontal()).getOpposite().data);
    }

    @Override
    public BlockState getStateForNeighborUpdate2(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        Direction facing = BlockUtil.getProperty(state, FACING);
        if (!WallAttachedBlockBehavior.canBePlaced(pos, World.cast(world), getWallDirection(facing))) {
            return Blocks.getAirMapped().getDefaultState();
        }

        return super.getStateForNeighborUpdate2(state, direction, neighborState, world, pos, neighborPos);
    }

    public Direction getWallDirection(Direction defaultDirection) {
        return defaultDirection;
    }
}
