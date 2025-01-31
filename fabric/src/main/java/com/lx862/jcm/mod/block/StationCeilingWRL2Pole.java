package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.PoleBlock;
import com.lx862.jcm.mod.data.BlockProperties;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.block.IBlock;

import java.util.List;

public class StationCeilingWRL2Pole extends PoleBlock {
    public static final BooleanProperty PART = BlockProperties.HORIZONTAL_IS_LEFT;

    public StationCeilingWRL2Pole(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        if(IBlock.getStatePropertySafe(state, PART)) {
            return IBlock.getVoxelShapeByDirection(5.5, 0, 7.5, 6.5, 16, 8.5, IBlock.getStatePropertySafe(state, FACING));
        } else {
            return IBlock.getVoxelShapeByDirection(10.5, 0, 7.5, 11.5, 16, 8.5, IBlock.getStatePropertySafe(state, FACING));
        }
    }


    @Override
    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        BlockState superState = super.getPlacementState2(ctx);
        if(superState == null) return null;

        BlockState blockBelow = ctx.getWorld().getBlockState(ctx.getBlockPos().down());
        return superState
                .with(new Property<>(PART.data), IBlock.getStatePropertySafe(blockBelow, PART))
                .with(new Property<>(FACING.data), IBlock.getStatePropertySafe(blockBelow, FACING).data);
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(PART);
    }

    @Override
    public boolean blockIsAllowed(Block block) {
        return block.data instanceof StationCeilingWRL2Block || block.data instanceof StationCeilingWRL2Pole;
    }
}
