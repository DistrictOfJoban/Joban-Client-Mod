package com.lx862.jcm.blocks.base;

import com.lx862.jcm.util.BlockUtil;
import net.minecraft.world.WorldView;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.holder.BlockSettings;

public class HorizontalWallAttachedMultiBlock extends HorizontalMultiBlock {

    public HorizontalWallAttachedMultiBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public boolean canPlaceAt2(BlockState state, WorldView world, BlockPos pos) {
        boolean willBeFullyAttached = WallAttachedBlock.isAttached(state, pos, world) && WallAttachedBlock.isAttached(state, pos.offset(BlockUtil.getStateProperty(state, FACING).rotateYClockwise()), world);
        return willBeFullyAttached && HorizontalMultiBlock.canPlace(state, world, pos, width);
    }

    @Override
    public BlockState getStateForNeighborUpdate2(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if(!HorizontalMultiBlock.isConnected(state, world, pos, width) || !WallAttachedBlock.isAttached(state, pos, world)) {
            return Blocks.getAirMapped().getDefaultState();
        }

        return super.getStateForNeighborUpdate2(state, direction, neighborState, world, pos, neighborPos);
    }
}
