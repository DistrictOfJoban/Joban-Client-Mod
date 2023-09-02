package com.lx862.jcm.blocks.base;

import com.lx862.jcm.blocks.data.BlockProperties;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public abstract class CeilingGroundAttachedDirectionalMultiBlock extends CeilingGroundAttachedDirectionalBlock {
    public static final int width = 2;
    public static final IntProperty PART = BlockProperties.HORIZONTAL_PART;
    public CeilingGroundAttachedDirectionalMultiBlock(Settings settings, boolean canAttachTop, boolean canAttachBottom) {
        super(settings, canAttachTop, canAttachBottom);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        boolean bl1 = HorizontalMultiBlock.canPlace(state, world, pos, width);
        boolean bl2 = super.canPlaceAt(state, world, pos) && super.canPlaceAt(state, world, pos.offset(state.get(FACING).rotateYClockwise()));
        return bl1 && bl2;
    }

    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        HorizontalMultiBlock.placeMultiBlock(world, pos, state, state.get(FACING).rotateYClockwise(), width);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if(!HorizontalMultiBlock.isConnected(state, world, pos, width)) {
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
