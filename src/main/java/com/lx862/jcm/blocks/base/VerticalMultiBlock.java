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

public class VerticalMultiBlock extends DirectionalBlock {
    private final int height;
    public static final IntProperty PART = BlockProperties.VERTICAL_PART;

    public VerticalMultiBlock(Settings settings, int height) {
        super(settings);
        this.height = height;
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        return BlockUtil.isReplacable(world, pos, Direction.UP, height);
    }

    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        for(int i = 0; i < height; i++) {
            if(i == 0) continue;
            world.setBlockState(pos.up(i), state.with(PART, i));
        }
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        int thisPart = state.get(PART);

        if(!BlockUtil.canSurvive(this, world, pos, Direction.UP, thisPart, height)) {
            return Blocks.AIR.getDefaultState();
        }

        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(PART);
    }
}
