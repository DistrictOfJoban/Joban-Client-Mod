package com.lx862.jcm.mod.block.base;

import com.lx862.jcm.mod.block.behavior.HorizontalMultiBlock;
import com.lx862.jcm.mod.util.BlockUtil;
import net.minecraft.world.WorldView;
import org.mtr.mapping.holder.*;

public abstract class HorizontalWallAttached2Block extends Horizontal2Block implements HorizontalMultiBlock {
    public HorizontalWallAttached2Block(BlockSettings settings) {
        super(settings);
    }
    
    @Override
    public boolean canPlaceAt2(BlockState state, WorldView world, BlockPos pos) {
        boolean willBeFullyAttached = WallAttachedBlock.isAttached(pos, world, BlockUtil.getProperty(state, FACING)) && WallAttachedBlock.isAttached(pos.offset(BlockUtil.getProperty(state, FACING).rotateYClockwise()), world, BlockUtil.getProperty(state, FACING));
        return willBeFullyAttached && HorizontalMultiBlock.canBePlaced(state, world, pos, width);
    }

    @Override
    public BlockState getStateForNeighborUpdate2(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if(!HorizontalMultiBlock.blockNotValid(pos, state, world, new Property<>(PART.data), width) || !WallAttachedBlock.isAttached(pos, world, BlockUtil.getProperty(state, FACING))) {
            return Blocks.getAirMapped().getDefaultState();
        }

        return super.getStateForNeighborUpdate2(state, direction, neighborState, world, pos, neighborPos);
    }
}
