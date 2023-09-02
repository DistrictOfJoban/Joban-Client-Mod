package com.lx862.jcm.blocks.base;

import com.lx862.jcm.blocks.data.BlockProperties;
import com.lx862.jcm.util.BlockUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public class HorizontalMultiBlock extends DirectionalBlock {
    public static final int width = 2;
    public static final IntProperty PART = BlockProperties.HORIZONTAL_PART;

    public HorizontalMultiBlock(Settings settings) {
        super(settings);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        return canPlace(state, world, pos, width);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        placeMultiBlock(world, pos, state, state.get(FACING).rotateYClockwise(), width);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if(!isConnected(state, world, pos, width)) {
            return Blocks.AIR.getDefaultState();
        }

        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(PART);
    }

    public static boolean canPlace(BlockState state, WorldView world, BlockPos pos, int width) {
        return BlockUtil.isReplacable(world, pos, state.get(FACING).rotateYClockwise(), width);
    }

    public static void placeMultiBlock(World world, BlockPos pos, BlockState state, Direction directionToPlace, int length) {
        for(int i = 0; i < length; i++) {
            if(i == 0) continue;
            world.setBlockState(pos.offset(directionToPlace, i), state.with(PART, i));
        }
    }

    public static boolean isConnected(BlockState state, WorldAccess world, BlockPos pos, int totalWidthHeight) {
        int thisPart = state.get(PART);
        return BlockUtil.canSurvive(state.getBlock(), world, pos, state.get(FACING).rotateYClockwise(), thisPart, totalWidthHeight);
    }
}
