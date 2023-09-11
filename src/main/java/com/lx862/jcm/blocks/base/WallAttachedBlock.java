package com.lx862.jcm.blocks.base;

import com.lx862.jcm.util.BlockUtil;
import net.minecraft.world.WorldView;
import org.mtr.mapping.holder.*;

public abstract class WallAttachedBlock extends DirectionalBlock {

    public WallAttachedBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public boolean canPlaceAt2(BlockState state, WorldView world, BlockPos pos) {
        return isAttached(state, pos, world);
    }

    @Override
    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        if (ctx.getSide() == Direction.DOWN || ctx.getSide() == Direction.UP) {
            return Blocks.getAirMapped().getDefaultState();
        }

        return super.getPlacementState2(ctx).with(new Property<>(FACING.data), ctx.getSide().getOpposite().data);
    }

    @Override
    public BlockState getStateForNeighborUpdate2(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (!isAttached(state, pos, world)) {
            return Blocks.getAirMapped().getDefaultState();
        }

        return super.getStateForNeighborUpdate2(state, direction, neighborState, world, pos, neighborPos);
    }

    public static boolean isAttached(BlockState state, BlockPos pos, WorldView world) {
        BlockPos blockBehindPos = pos.offset(BlockUtil.getProperty(state, FACING));
        BlockState blockBehind = new BlockState(world.getBlockState(blockBehindPos.data));

        return BlockUtil.blockConsideredSolid(blockBehind);
    }

    public static boolean isAttached(BlockState state, BlockPos pos, WorldAccess world) {
        BlockPos blockBehindPos = pos.offset(BlockUtil.getProperty(state, FACING));
        BlockState blockBehind = world.getBlockState(blockBehindPos);

        return BlockUtil.blockConsideredSolid(blockBehind);
    }
}
