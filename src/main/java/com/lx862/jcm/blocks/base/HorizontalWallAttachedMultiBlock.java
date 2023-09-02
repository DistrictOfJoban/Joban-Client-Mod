package com.lx862.jcm.blocks.base;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public class HorizontalWallAttachedMultiBlock extends HorizontalMultiBlock {

    public HorizontalWallAttachedMultiBlock(Settings settings) {
        super(settings);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        boolean willBeFullyAttached = WallAttachedBlock.isAttached(state, pos, world) && WallAttachedBlock.isAttached(state, pos.offset(state.get(FACING).rotateYClockwise()), world);
        return willBeFullyAttached && HorizontalMultiBlock.canPlace(state, world, pos, width);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if(!HorizontalMultiBlock.isConnected(state, world, pos, width) || !WallAttachedBlock.isAttached(state, pos, world)) {
            return Blocks.AIR.getDefaultState();
        }

        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }
}
