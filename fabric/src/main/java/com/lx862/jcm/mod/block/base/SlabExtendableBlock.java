package com.lx862.jcm.mod.block.base;

import com.lx862.jcm.mod.data.BlockProperties;
import net.minecraft.block.enums.SlabType;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;

import java.util.List;

public abstract class SlabExtendableBlock extends DirectionalBlock {
    public static final BooleanProperty HAS_TOP = BlockProperties.HAS_TOP;

    public SlabExtendableBlock(BlockSettings settings) {
        super(settings);
        setDefaultState2(getDefaultState2().with(new Property<>(HAS_TOP.data), false));
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(HAS_TOP);
    }

    @Override
    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        return super.getPlacementState2(ctx) == null ? null : super.getPlacementState2(ctx).with(new Property<>(HAS_TOP.data), shouldExtendForSlab(WorldAccess.cast(ctx.getWorld()), ctx.getBlockPos()));
    }

    @Override
    public BlockState getStateForNeighborUpdate2(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        return super.getStateForNeighborUpdate2(state, direction, neighborState, world, pos, neighborPos).with(new Property<>(HAS_TOP.data), shouldExtendForSlab(world, pos));
    }

    public static boolean shouldExtendForSlab(WorldAccess world, BlockPos pos) {
        BlockState blockTop = world.getBlockState(pos.up());

        if (blockTop.getBlock().data instanceof net.minecraft.block.SlabBlock) {
            return blockTop.get(new Property<>(net.minecraft.block.SlabBlock.TYPE)) == SlabType.TOP;
        } else {
            return false;
        }
    }
}
