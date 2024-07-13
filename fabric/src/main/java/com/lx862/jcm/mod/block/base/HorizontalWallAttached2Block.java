package com.lx862.jcm.mod.block.base;

import com.lx862.jcm.mod.block.behavior.HorizontalDoubleBlockBehavior;
import com.lx862.jcm.mod.block.behavior.WallAttachedBlockBehavior;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.block.IBlock;

import java.util.List;

public abstract class HorizontalWallAttached2Block extends DirectionalBlock implements HorizontalDoubleBlockBehavior {
    public HorizontalWallAttached2Block(BlockSettings settings) {
        super(settings);
    }

    @Override
    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        BlockState superState = super.getPlacementState2(ctx);
        if(superState == null) return null;

        boolean firstBlockAttachable = WallAttachedBlockBehavior.canBePlaced(ctx.getBlockPos(), ctx.getWorld(), IBlock.getStatePropertySafe(superState, FACING));
        boolean secondBlockAttachable = WallAttachedBlockBehavior.canBePlaced(ctx.getBlockPos().offset(IBlock.getStatePropertySafe(superState, FACING).rotateYClockwise()), ctx.getWorld(), IBlock.getStatePropertySafe(superState, FACING));

        return firstBlockAttachable && secondBlockAttachable && HorizontalDoubleBlockBehavior.canBePlaced(ctx) ? super.getPlacementState2(ctx) : null;
    }

    @Override
    public void onPlaced2(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        HorizontalDoubleBlockBehavior.onPlaced(world, state, pos);
    }

    @Override
    public BlockState getStateForNeighborUpdate2(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        boolean isLeft = IBlock.getStatePropertySafe(state, IS_LEFT);

        boolean blockAttachable = WallAttachedBlockBehavior.canBePlaced(pos, World.cast(world), IBlock.getStatePropertySafe(state, FACING));

        if(!HorizontalDoubleBlockBehavior.blockIsValid(pos, state, world, isLeft) || !blockAttachable) {
            return Blocks.getAirMapped().getDefaultState();
        }

        return super.getStateForNeighborUpdate2(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public BlockPos[] getAllPos(BlockState state, World world, BlockPos pos) {
        Direction facing = IBlock.getStatePropertySafe(state, FACING);
        BlockPos otherPos = pos.offset(facing);
        return new BlockPos[]{ pos, otherPos };
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        HorizontalDoubleBlockBehavior.addProperties(properties);
    }
}
