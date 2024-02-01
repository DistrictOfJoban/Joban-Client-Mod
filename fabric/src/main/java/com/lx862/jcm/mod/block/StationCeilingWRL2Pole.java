package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.PoleBlock;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.VoxelUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;

import java.util.List;

public class StationCeilingWRL2Pole extends PoleBlock {
    public static final BooleanProperty PART = BlockProperties.HORIZONTAL_PART_LEFT;

    public StationCeilingWRL2Pole(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        if(BlockUtil.getProperty(state, PART)) {
            return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 5.5, 0, 7.5, 6.5, 16, 8.5);
        } else {
            return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 10.5, 0, 7.5, 11.5, 16, 8.5);
        }
    }


    @Override
    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        BlockState superState = super.getPlacementState2(ctx);
        if(superState == null) return null;

        BlockState blockBelow = ctx.getWorld().getBlockState(ctx.getBlockPos().down());
        return superState
                .with(new Property<>(PART.data), BlockUtil.getProperty(blockBelow, PART))
                .with(new Property<>(FACING.data), BlockUtil.getProperty(blockBelow, FACING).data);
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
