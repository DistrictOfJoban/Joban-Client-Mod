package com.lx862.jcm.mod.block.base;

import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.*;

public abstract class WallAttachedBlock extends DirectionalBlock {
    public WallAttachedBlock(BlockSettings settings) {
        super(settings);
    }

    public boolean canPlace(BlockState state, World world, BlockPos pos) {
        if(state == null) return false;
        Direction facing = BlockUtil.getProperty(state, FACING);
        return isAttached(pos, world, getOffsetDirection(facing));
    }

    @Override
    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        if (super.getPlacementState2(ctx) == null) return null;
        if(!canPlace(super.getPlacementState2(ctx), ctx.getWorld(), ctx.getBlockPos())) return null;

        return super.getPlacementState2(ctx).with(new Property<>(FACING.data), Direction.fromHorizontal(ctx.getSide().getHorizontal()).getOpposite().data);
    }

    @Override
    public BlockState getStateForNeighborUpdate2(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        Direction facing = BlockUtil.getProperty(state, FACING);
        if (!isAttached(pos, world, getOffsetDirection(facing))) {
            return Blocks.getAirMapped().getDefaultState();
        }

        return super.getStateForNeighborUpdate2(state, direction, neighborState, world, pos, neighborPos);
    }

    public static boolean isAttached(BlockPos pos, World world, Direction offsetDirection) {
        BlockPos attachedBlockPos = pos.offset(offsetDirection);
        BlockState attachedBlock = world.getBlockState(attachedBlockPos);

        return BlockUtil.blockConsideredSolid(attachedBlock);
    }

    public static boolean isAttached(BlockPos pos, WorldAccess world, Direction offsetDirection) {
        return isAttached(pos, World.cast(world), offsetDirection);
    }

    public Direction getOffsetDirection(Direction defaultDirection) {
        return defaultDirection;
    }
}
