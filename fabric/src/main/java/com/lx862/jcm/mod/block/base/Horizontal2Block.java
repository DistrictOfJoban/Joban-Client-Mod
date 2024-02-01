package com.lx862.jcm.mod.block.base;

import com.lx862.jcm.mod.block.behavior.HorizontalMultiBlock;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;

import java.util.List;

public abstract class Horizontal2Block extends DirectionalBlock implements HorizontalMultiBlock {
    public static final int width = 2;
    public static final BooleanProperty IS_LEFT = BlockProperties.HORIZONTAL_PART_LEFT;

    public Horizontal2Block(BlockSettings settings) {
        super(settings);
    }

    @Override
    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        World world = ctx.getWorld();
        for(int i = 0; i < 2; i++) {
            BlockState bs = world.getBlockState(ctx.getBlockPos().offset(ctx.getPlayerFacing().rotateYClockwise(), i));
            if(!bs.canReplace(ctx)) return null;
        }

        return super.getPlacementState2(ctx);
    }

    @Override
    public void onPlaced2(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        world.setBlockState(pos.offset(BlockUtil.getProperty(state, FACING).rotateYClockwise()), state.with(new Property<>(IS_LEFT.data), false));
    }

    @Override
    public BlockState getStateForNeighborUpdate2(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        boolean isLeft = BlockUtil.getProperty(state, IS_LEFT);

        if(!HorizontalMultiBlock.blockIsValid(pos, state, world, !isLeft, isLeft)) {
            return Blocks.getAirMapped().getDefaultState();
        }

        return super.getStateForNeighborUpdate2(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(IS_LEFT);
    }
}
