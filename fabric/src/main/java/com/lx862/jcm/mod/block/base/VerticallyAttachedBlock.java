package com.lx862.jcm.mod.block.base;

import com.lx862.jcm.mod.data.BlockProperties;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;

import java.util.List;

public abstract class VerticallyAttachedBlock extends JCMBlock {
    public static final BooleanProperty TOP = BlockProperties.TOP;
    protected final boolean canAttachTop;
    protected final boolean canAttachBottom;

    public VerticallyAttachedBlock(BlockSettings settings, boolean canAttachTop, boolean canAttachBottom) {
        super(settings);
        setDefaultState2(getDefaultState2().with(new Property<>(TOP.data), false));
        this.canAttachTop = canAttachTop;
        this.canAttachBottom = canAttachBottom;
    }

    @Override
    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        BlockState superState = super.getPlacementState2(ctx);
        BlockState blockAbove = ctx.getWorld().getBlockState(ctx.getBlockPos().up());
        if(superState == null) return null;
        if(!com.lx862.jcm.mod.block.behavior.VerticallyAttachedBlock.canPlace(canAttachTop, canAttachBottom, ctx)) return null;

        return superState.with(new Property<>(TOP.data), !blockAbove.isAir());
    }

    @Override
    public BlockState getStateForNeighborUpdate2(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        BlockState attachedBlock = state.get(new Property<>(TOP.data)) ? world.getBlockState(pos.up()) : world.getBlockState(pos.down());

        if (shouldBreakOnBlockUpdate() && attachedBlock.isAir()) {
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
