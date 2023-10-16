package com.lx862.jcm.mod.block.base;

import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.util.BlockUtil;
import net.minecraft.world.WorldView;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;

import java.util.List;

public abstract class VerticallyAttachedBlock extends WaterloggableBlock {
    public static final BooleanProperty TOP = BlockProperties.TOP;
    private final boolean canAttachTop;
    private final boolean canAttachBottom;

    public VerticallyAttachedBlock(BlockSettings settings, boolean canAttachTop, boolean canAttachBottom) {
        super(settings);
        setDefaultState2(getDefaultState2().with(new Property<>(TOP.data), false));
        this.canAttachTop = canAttachTop;
        this.canAttachBottom = canAttachBottom;
    }

    @Override
    public boolean canPlaceAt2(BlockState state, WorldView world, org.mtr.mapping.holder.BlockPos pos) {
        boolean blockAboveSolid = BlockUtil.blockConsideredSolid(new BlockState(world.getBlockState(pos.up().data)));
        boolean blockBelowSolid = BlockUtil.blockConsideredSolid(new BlockState(world.getBlockState(pos.down().data)));

        if (!blockAboveSolid && !blockBelowSolid) return false;
        if (!canAttachTop && !blockBelowSolid) return false;
        return canAttachBottom || blockAboveSolid;
    }

    @Override
    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        BlockState blockAbove = ctx.getWorld().getBlockState(ctx.getBlockPos().up());

        return super.getPlacementState2(ctx).with(new Property<>(TOP.data), BlockUtil.blockConsideredSolid(blockAbove));
    }

    @Override
    public BlockState getStateForNeighborUpdate2(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        BlockState attachedBlock = state.get(new Property<>(TOP.data)) ? world.getBlockState(pos.up()) : world.getBlockState(pos.down());

        if (shouldBreakOnBlockUpdate() && !BlockUtil.blockConsideredSolid(attachedBlock)) {
            return Blocks.getAirMapped().getDefaultState();
        }

        return super.getStateForNeighborUpdate2(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(TOP);
    }

    protected boolean shouldBreakOnBlockUpdate() {
        return true;
    }
}
