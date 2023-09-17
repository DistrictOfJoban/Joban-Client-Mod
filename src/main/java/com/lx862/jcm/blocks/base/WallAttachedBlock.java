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
        Direction facing = BlockUtil.getProperty(state, FACING);
        return isAttached(pos, world, getOffsetDirection(facing));
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
        Direction facing = BlockUtil.getProperty(state, FACING);
        if (!isAttached(pos, world, getOffsetDirection(facing))) {
            return Blocks.getAirMapped().getDefaultState();
        }

        return super.getStateForNeighborUpdate2(state, direction, neighborState, world, pos, neighborPos);
    }

    public static boolean isAttached(BlockPos pos, WorldView world, Direction offsetDirection) {
        BlockPos attachedBlockPos = pos.offset(offsetDirection);
        BlockState attachedBlock = new BlockState(world.getBlockState(attachedBlockPos.data));

        return BlockUtil.blockConsideredSolid(attachedBlock);
    }

    public static boolean isAttached(BlockPos pos, WorldAccess world, Direction offsetDirection) {
        return isAttached(pos, world.data, offsetDirection);
    }

    public Direction getOffsetDirection(Direction defaultDirection) {
        return defaultDirection;
    }
}
