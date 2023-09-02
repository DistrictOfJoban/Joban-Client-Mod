package com.lx862.jcm.blocks.base;

import com.lx862.jcm.util.BlockUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public abstract class CeilingGroundAttachedBlock extends WaterloggableBlock {
    public static final BooleanProperty TOP = BooleanProperty.of("top");
    private final boolean canAttachTop;
    private final boolean canAttachBottom;

    public CeilingGroundAttachedBlock(Settings settings, boolean canAttachTop, boolean canAttachBottom) {
        super(settings);
        setDefaultState(getDefaultState().with(TOP, false));
        this.canAttachTop = canAttachTop;
        this.canAttachBottom = canAttachBottom;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(TOP);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        boolean blockAboveSolid = BlockUtil.blockConsideredSolid(world.getBlockState(pos.up()));
        boolean blockBelowSolid = BlockUtil.blockConsideredSolid(world.getBlockState(pos.down()));

        if(!blockAboveSolid && !blockBelowSolid) return false;
        if(!canAttachTop && !blockBelowSolid) return false;
        if(!canAttachBottom && !blockAboveSolid) return false;

        return true;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState blockAbove = ctx.getWorld().getBlockState(ctx.getBlockPos().up());

        return super.getPlacementState(ctx).with(TOP, BlockUtil.blockConsideredSolid(blockAbove));
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        BlockState attachedBlock = state.get(TOP) ? world.getBlockState(pos.up()) : world.getBlockState(pos.down());

        if(shouldBreakOnBlockUpdate() && !BlockUtil.blockConsideredSolid(attachedBlock)) {
            return Blocks.AIR.getDefaultState();
        }

        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    protected abstract boolean shouldBreakOnBlockUpdate();
}
