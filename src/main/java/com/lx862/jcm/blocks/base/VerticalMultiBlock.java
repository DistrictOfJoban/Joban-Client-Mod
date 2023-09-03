package com.lx862.jcm.blocks.base;

import com.lx862.jcm.blocks.data.BlockProperties;
import com.lx862.jcm.util.BlockUtil;
import net.minecraft.world.WorldView;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;

import java.util.List;

public class VerticalMultiBlock extends DirectionalBlock {
    private final int height;
    public static final IntegerProperty PART = BlockProperties.VERTICAL_PART;

    public VerticalMultiBlock(BlockSettings settings, int height) {
        super(settings);
        this.height = height;
    }

    @Override
    public boolean canPlaceAt2(BlockState state, WorldView world, BlockPos pos) {
        return BlockUtil.isReplacable(world, pos, Direction.UP, height);
    }

    @Override
    public void onPlaced2(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        for (int i = 0; i < height; i++) {
            if (i == 0) continue;
            world.setBlockState(pos.up(i), state.with(new Property<>(PART.data), i));
        }
    }

    @Override
    public BlockState getStateForNeighborUpdate2(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        int thisPart = state.get(new Property<>(PART.data));

        if (!BlockUtil.canSurvive(this, world, pos, Direction.UP, thisPart, height)) {
            return Blocks.getAirMapped().getDefaultState();
        }

        return super.getStateForNeighborUpdate2(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(PART);
    }
}
