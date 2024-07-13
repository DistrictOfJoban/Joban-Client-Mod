package com.lx862.jcm.mod.block.base;

import com.lx862.jcm.mod.block.behavior.VerticalDoubleBlock;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.block.IBlock;

import java.util.List;

public abstract class Vertical2Block extends DirectionalBlock implements VerticalDoubleBlock {

    public Vertical2Block(BlockSettings settings) {
        super(settings);
    }
    @Override
    public void onPlaced2(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        VerticalDoubleBlock.placeBlock(world, pos, state);
    }

    @Override
    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        BlockState superState = super.getPlacementState2(ctx);
        if(superState == null) return null;
        return VerticalDoubleBlock.canBePlaced(ctx.getWorld(), ctx.getBlockPos(), ctx) ? superState.with(new Property<>(HALF.data), IBlock.DoubleBlockHalf.LOWER) : null;
    }

    @Override
    public BlockState getStateForNeighborUpdate2(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if(VerticalDoubleBlock.blockNotValid(new Block(this), world, pos, state)) {
            return Blocks.getAirMapped().getDefaultState();
        }

        return super.getStateForNeighborUpdate2(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public BlockPos[] getAllPos(BlockState state, World world, BlockPos pos) {
        switch(IBlock.getStatePropertySafe(state, new Property<>(HALF.data))) {
            case LOWER:
                return new BlockPos[]{
                        pos,
                        pos.up()};
            case UPPER:
                return new BlockPos[]{
                        pos.down(),
                        pos};
            default:
                return super.getAllPos(state, world, pos);
        }
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(HALF);
    }
}
