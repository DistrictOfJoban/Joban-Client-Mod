package com.lx862.jcm.mod.block.base;

import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.*;

public abstract class WallAttachedBlock extends DirectionalBlock {
    public WallAttachedBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        BlockState superState = super.getPlacementState2(ctx);
        if (superState == null) return null;

        Direction facing = BlockUtil.getProperty(superState, FACING);
        boolean isAttached = isAttached(ctx.getBlockPos(), World.cast(ctx.getWorld()), getWallDirection(facing));
        if(!isAttached) return null;

        return superState.with(new Property<>(FACING.data), Direction.fromHorizontal(ctx.getSide().getHorizontal()).getOpposite().data);
    }

    @Override
    public BlockState getStateForNeighborUpdate2(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        Direction facing = BlockUtil.getProperty(state, FACING);
        if (!isAttached(pos, World.cast(world), getWallDirection(facing))) {
            return Blocks.getAirMapped().getDefaultState();
        }

        return super.getStateForNeighborUpdate2(state, direction, neighborState, world, pos, neighborPos);
    }

    public static boolean isAttached(BlockPos pos, World world, Direction wallDirection) {
        BlockPos wallBlockPos = pos.offset(wallDirection);
        BlockState wallBlock = world.getBlockState(wallBlockPos);

        return BlockUtil.blockConsideredSolid(wallBlock);
    }

    public Direction getWallDirection(Direction defaultDirection) {
        return defaultDirection;
    }
}
