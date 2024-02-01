package com.lx862.jcm.mod.block.base;

import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.*;

/**
 * Cheap way for a horizontal multi block, with 2 same block facing each other.
 * @deprecated Only retained for backward compatibility reason, please use Horizontal2Block for all future blocks.
 */
public abstract class Horizontal2MirroredBlock extends DirectionalBlock {
    public Horizontal2MirroredBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        Direction placeDirection = ctx.getPlayerFacing().getOpposite();
        return BlockUtil.isReplacable(ctx.getWorld(), ctx.getBlockPos(), placeDirection, ctx, 2) && super.getPlacementState2(ctx) != null ? super.getPlacementState2(ctx).with(new Property<>(FACING.data), placeDirection.data) : null;
    }

    @Override
    public void onPlaced2(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        if(world.isClient()) return;

        Direction facing = BlockUtil.getProperty(state, FACING);
        world.setBlockState(pos.offset(facing), state.with(new Property<>(FACING.data), facing.getOpposite().data), 3);
    }

    @Override
    public BlockState getStateForNeighborUpdate2(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if(BlockUtil.getProperty(state, FACING) == direction && !(neighborState.isOf(new Block(this)))) {
            return Blocks.getAirMapped().getDefaultState();
        }
        return state;
    }
}