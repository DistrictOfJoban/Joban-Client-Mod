package com.lx862.jcm.blocks.base;

import com.lx862.jcm.blocks.data.BlockProperties;
import com.lx862.jcm.util.BlockUtil;
import net.minecraft.world.WorldView;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;

import java.util.List;

public abstract class CeilingGroundAttachedDirectionalMultiBlock extends CeilingGroundAttachedDirectionalBlock {
    public static final int width = 2;
    public static final IntegerProperty PART = BlockProperties.HORIZONTAL_PART;
    public CeilingGroundAttachedDirectionalMultiBlock(BlockSettings settings, boolean canAttachTop, boolean canAttachBottom) {
        super(settings, canAttachTop, canAttachBottom);
    }

    @Override
    public boolean canPlaceAt2(BlockState state, WorldView world, BlockPos pos) {
        boolean bl1 = HorizontalMultiBlock.canPlace(state, world, pos, width);
        boolean bl2 = super.canPlaceAt2(state, world, pos) && super.canPlaceAt2(state, world, pos.offset(BlockUtil.getStateProperty(state, FACING).rotateYClockwise()));
        return bl1 && bl2;
    }

    public void onPlaced2(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        HorizontalMultiBlock.placeMultiBlock(world, pos, state, Direction.convert(state.get(new Property<>(FACING.data)).rotateYClockwise()), width);
    }

    @Override
    public BlockState getStateForNeighborUpdate2(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if(!HorizontalMultiBlock.isConnected(state, world, pos, width)) {
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
