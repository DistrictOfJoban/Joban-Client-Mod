package com.lx862.jcm.mod.block.base;

import com.lx862.jcm.mod.block.behavior.HorizontalMultiBlock;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.*;

public abstract class HorizontalWallAttached2Block extends Horizontal2Block implements HorizontalMultiBlock {
    public HorizontalWallAttached2Block(BlockSettings settings) {
        super(settings);
    }

    public boolean canPlace(BlockState blockState, ItemPlacementContext ctx) {
        if(blockState == null) return false;
        boolean firstBlockCanAttach = WallAttachedBlock.isAttached(ctx.getBlockPos(), ctx.getWorld(), BlockUtil.getProperty(blockState, FACING));
        boolean secondBlockCanAttach = WallAttachedBlock.isAttached(ctx.getBlockPos().offset(BlockUtil.getProperty(blockState, FACING).rotateYClockwise()), ctx.getWorld(), BlockUtil.getProperty(blockState, FACING));

        return firstBlockCanAttach && secondBlockCanAttach && HorizontalMultiBlock.canBePlaced(blockState, ctx, width);
    }

    @Override
    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        return canPlace(super.getPlacementState2(ctx), ctx) ? super.getPlacementState2(ctx) : null;
    }

    @Override
    public BlockState getStateForNeighborUpdate2(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        boolean isLeft = BlockUtil.getProperty(state, IS_LEFT);
        if(!HorizontalMultiBlock.blockIsValid(pos, state, world, !isLeft, isLeft) || !WallAttachedBlock.isAttached(pos, world, BlockUtil.getProperty(state, FACING))) {
            return Blocks.getAirMapped().getDefaultState();
        }

        return super.getStateForNeighborUpdate2(state, direction, neighborState, world, pos, neighborPos);
    }
}
