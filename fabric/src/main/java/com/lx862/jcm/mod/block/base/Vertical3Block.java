package com.lx862.jcm.mod.block.base;

import com.lx862.jcm.mod.block.behavior.VerticalMultiBlock;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;

import java.util.List;

public abstract class Vertical3Block extends DirectionalBlock implements VerticalMultiBlock {
    private static final int height = 3;
    public static final IntegerProperty PART = BlockProperties.VERTICAL_PART_3;

    public Vertical3Block(BlockSettings settings) {
        super(settings);
    }

    @Override
    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        return VerticalMultiBlock.canBePlaced(ctx.getWorld(), ctx.getBlockPos(), ctx, height) ? super.getPlacementState2(ctx) : null;
    }

    @Override
    public void onPlaced2(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        VerticalMultiBlock.placeBlock(world, pos, state, new Property<>(PART.data), height);
    }

    @Override
    public BlockState getStateForNeighborUpdate2(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if(VerticalMultiBlock.blockNotValid(world, pos, state, new Property<>(PART.data), height)) {
            return Blocks.getAirMapped().getDefaultState();
        }

        return super.getStateForNeighborUpdate2(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(PART);
    }

    @Override
    protected BlockEntity[] getBlockEntity(BlockState state, World world, BlockPos pos) {
        BlockPos above = pos.up();
        BlockPos below = pos.down();
        // HACK: To be replaced after PART state get replaced
        BlockPos above2 = above.up();
        BlockPos below2 = below.down();
        System.out.println("Hello");
        return new BlockEntity[]{
                BlockUtil.getBlockEntityOrNull(world, below2),
                BlockUtil.getBlockEntityOrNull(world, below),
                BlockUtil.getBlockEntityOrNull(world, pos),
                BlockUtil.getBlockEntityOrNull(world, above),
                BlockUtil.getBlockEntityOrNull(world, above2)};
    }
}
