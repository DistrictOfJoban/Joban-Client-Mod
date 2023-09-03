package com.lx862.jcm.blocks.base;

import com.lx862.jcm.util.BlockUtil;
import net.minecraft.world.WorldView;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;

import java.util.List;

public abstract class CeilingGroundAttachedBlock extends WaterloggableBlock {
    public static final BooleanProperty TOP = BooleanProperty.of("top");
    private final boolean canAttachTop;
    private final boolean canAttachBottom;

    public CeilingGroundAttachedBlock(BlockSettings settings, boolean canAttachTop, boolean canAttachBottom) {
        super(settings);
        setDefaultState2(getDefaultState2().with(new Property<>(TOP.data), false));
        this.canAttachTop = canAttachTop;
        this.canAttachBottom = canAttachBottom;
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties)  {
        super.addBlockProperties(properties);
        properties.add(TOP);
    }

    @Override
    public boolean canPlaceAt2(org.mtr.mapping.holder.BlockState state, WorldView world, org.mtr.mapping.holder.BlockPos pos) {
        boolean blockAboveSolid = BlockUtil.blockConsideredSolid(new BlockState(world.getBlockState(pos.up().data)));
        boolean blockBelowSolid = BlockUtil.blockConsideredSolid(new BlockState(world.getBlockState(pos.down().data)));

        if(!blockAboveSolid && !blockBelowSolid) return false;
        if(!canAttachTop && !blockBelowSolid) return false;
        if(!canAttachBottom && !blockAboveSolid) return false;

        return true;
    }

    @Override
    public org.mtr.mapping.holder.BlockState getPlacementState2(ItemPlacementContext ctx) {
        org.mtr.mapping.holder.BlockState blockAbove = ctx.getWorld().getBlockState(ctx.getBlockPos().up());

        return super.getPlacementState2(ctx).with(new Property<>(TOP.data), BlockUtil.blockConsideredSolid(blockAbove));
    }

    @Override
    public org.mtr.mapping.holder.BlockState getStateForNeighborUpdate2(org.mtr.mapping.holder.BlockState state, Direction direction, org.mtr.mapping.holder.BlockState neighborState, WorldAccess world, org.mtr.mapping.holder.BlockPos pos, BlockPos neighborPos) {
        org.mtr.mapping.holder.BlockState attachedBlock = state.get(new Property<>(TOP.data)) ? world.getBlockState(pos.up()) : world.getBlockState(pos.down());

        if(shouldBreakOnBlockUpdate() && !BlockUtil.blockConsideredSolid(attachedBlock)) {
            return Blocks.getAirMapped().getDefaultState();
        }

        return super.getStateForNeighborUpdate2(state, direction, neighborState, world, pos, neighborPos);
    }

    protected abstract boolean shouldBreakOnBlockUpdate();
}
