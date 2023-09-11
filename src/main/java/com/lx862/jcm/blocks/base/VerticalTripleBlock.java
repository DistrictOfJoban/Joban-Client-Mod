package com.lx862.jcm.blocks.base;

import com.lx862.jcm.blocks.data.BlockProperties;
import com.lx862.jcm.util.BlockUtil;
import net.minecraft.world.WorldView;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;

import java.util.List;

public abstract class VerticalTripleBlock extends VerticalMultiBlockBase {
    private static final int height = 3;
    public static final IntegerProperty PART = BlockProperties.VERTICAL_PART_3;

    public VerticalTripleBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public boolean canPlaceAt2(BlockState state, WorldView world, BlockPos pos) {
        return VerticalMultiBlockBase.canBePlaced(state, world, pos, height);
    }

    @Override
    public void onPlaced2(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        VerticalMultiBlockBase.placeAllBlock(world, pos, state, new Property<>(PART.data), height);
    }

    @Override
    public BlockState getStateForNeighborUpdate2(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if(VerticalMultiBlockBase.shouldRemove(world, pos, state, this, new Property<>(PART.data), height)) {
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
