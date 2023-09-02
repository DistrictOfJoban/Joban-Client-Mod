package com.lx862.jcm.blocks.base;

import com.lx862.jcm.blocks.data.BlockProperties;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public class SlabExtendibleBlock extends DirectionalBlock {
    public static final BooleanProperty HAS_TOP = BlockProperties.HAS_TOP;
    public SlabExtendibleBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(HAS_TOP, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(HAS_TOP);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return super.getPlacementState(ctx).with(HAS_TOP, shouldExtendForSlab(ctx.getWorld(), ctx.getBlockPos()));
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos).with(HAS_TOP, shouldExtendForSlab(world, pos));
    }

    public static boolean shouldExtendForSlab(WorldView world, BlockPos pos) {
        BlockState blockTop = world.getBlockState(pos.up());

        if(blockTop.getBlock() instanceof SlabBlock) {
            return blockTop.get(SlabBlock.TYPE) == SlabType.TOP;
        } else {
            return false;
        }
    }
}
