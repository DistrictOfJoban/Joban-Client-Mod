package com.lx862.jcm.blocks.base;

import com.lx862.jcm.blocks.data.BlockProperties;
import com.lx862.jcm.util.BlockUtil;
import net.minecraft.world.WorldView;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;

import java.util.List;

public class HorizontalMultiBlock extends DirectionalBlock {
    public static final int width = 2;
    public static final IntegerProperty PART = BlockProperties.HORIZONTAL_PART;

    public HorizontalMultiBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public boolean canPlaceAt2(BlockState state, WorldView world, org.mtr.mapping.holder.BlockPos pos) {
        return canPlace(state, world, pos, width);
    }

    @Override
    public void onPlaced2(World world, org.mtr.mapping.holder.BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        placeMultiBlock(world, pos, state, Direction.convert(state.get(new Property<>(FACING.data)).rotateYClockwise()), width);
    }

    @Override
    public BlockState getStateForNeighborUpdate2(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if(!isConnected(state, world, pos, width)) {
            return Blocks.getAirMapped().getDefaultState();
        }

        return super.getStateForNeighborUpdate2(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(PART);
    }

    public static boolean canPlace(BlockState state, WorldView world, BlockPos pos, int width) {
        return BlockUtil.isReplacable(world, pos, BlockUtil.getStateProperty(state, FACING).rotateYClockwise(), width);
    }

    public static void placeMultiBlock(World world, BlockPos pos, BlockState state, Direction directionToPlace, int length) {
        for(int i = 0; i < length; i++) {
            if(i == 0) continue;
            world.setBlockState(pos.offset(directionToPlace, i), state.with(new Property<>(PART.data), i));
        }
    }

    public static boolean isConnected(BlockState state, org.mtr.mapping.holder.WorldAccess world, org.mtr.mapping.holder.BlockPos pos, int totalWidthHeight) {
        int thisPart = state.get(new Property<>(PART.data));
        return BlockUtil.canSurvive(state.getBlock(), world, pos, BlockUtil.getStateProperty(state, FACING).rotateYClockwise(), thisPart, totalWidthHeight);
    }
}
