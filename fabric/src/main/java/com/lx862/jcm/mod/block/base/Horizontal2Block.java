package com.lx862.jcm.mod.block.base;

import com.lx862.jcm.mod.block.behavior.HorizontalDoubleBlockBehavior;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.block.IBlock;

import java.util.List;

public abstract class Horizontal2Block extends DirectionalBlock implements HorizontalDoubleBlockBehavior {
    public Horizontal2Block(BlockSettings settings) {
        super(settings);
    }

    @Override
    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        return HorizontalDoubleBlockBehavior.canBePlaced(ctx) ? super.getPlacementState2(ctx) : null;
    }

    @Override
    public void onPlaced2(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        HorizontalDoubleBlockBehavior.onPlaced(world, state, pos);
    }

    @Override
    public void onBreak2(World world, BlockPos breakPos, BlockState breakState, PlayerEntity player) {
        breakWithoutDropIfCreative(world, breakPos, breakState, player, this, HorizontalDoubleBlockBehavior::getLootDropPos);
        super.onBreak2(world, breakPos, breakState, player);
    }

    @Override
    public BlockState getStateForNeighborUpdate2(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        boolean isLeft = IBlock.getStatePropertySafe(state, IS_LEFT);
        if(!HorizontalDoubleBlockBehavior.blockIsValid(pos, state, world, isLeft)) return Blocks.getAirMapped().getDefaultState();

        return super.getStateForNeighborUpdate2(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        HorizontalDoubleBlockBehavior.addProperties(properties);
    }

    @Override
    public BlockPos[] getAllPos(BlockState state, WorldAccess world, BlockPos pos) {
        Direction facing = IBlock.getStatePropertySafe(state, FACING);
        boolean isLeft = IBlock.getStatePropertySafe(state, IS_LEFT);

        BlockPos secondaryPos = isLeft ? pos.offset(facing.rotateYClockwise()) : pos.offset(facing.rotateYCounterclockwise());
        return new BlockPos[]{ pos, secondaryPos };
    }
}
