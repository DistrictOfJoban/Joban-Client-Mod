package com.lx862.jcm.blocks.base;

import com.lx862.jcm.util.BlockUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public class WallAttachedBlock extends DirectionalBlock {

    public WallAttachedBlock(Settings settings) {
        super(settings);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        return isAttached(state, pos, world);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        if(ctx.getSide() == Direction.DOWN || ctx.getSide() == Direction.UP) return null;

        return super.getPlacementState(ctx).with(FACING, ctx.getSide().getOpposite());
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if(!isAttached(state, pos, world)) {
            return Blocks.AIR.getDefaultState();
        }

        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    public static boolean isAttached(BlockState state, BlockPos pos, WorldView world) {
        BlockPos blockBehindPos = pos.offset(state.get(FACING));
        BlockState blockBehind = world.getBlockState(blockBehindPos);

        return BlockUtil.blockConsideredSolid(blockBehind);
    }
}
