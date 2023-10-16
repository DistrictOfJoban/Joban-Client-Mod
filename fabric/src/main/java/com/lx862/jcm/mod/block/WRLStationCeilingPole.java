package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.PoleBlock;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.VoxelUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;

import java.util.List;

public class WRLStationCeilingPole extends PoleBlock {
    public static final IntegerProperty PART = BlockProperties.HORIZONTAL_PART;

    public WRLStationCeilingPole(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        switch (BlockUtil.getProperty(state, PART)) {
            case 0:
                return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 5.5, 0, 7.5, 6.5, 16, 8.5);
            case 1:
                return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 10.5, 0, 7.5, 11.5, 16, 8.5);
            default:
                return VoxelShapes.empty();
        }
    }


    @Override
    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        BlockState blockBelow = ctx.getWorld().getBlockState(ctx.getBlockPos().down());
        if ((blockBelow.getBlock().data instanceof WRLStationCeilingPole) || blockIsAllowed(blockBelow.getBlock())) {
            return super.getPlacementState2(ctx).with(new Property<>(PART.data), BlockUtil.getProperty(blockBelow, PART)).with(new Property<>(FACING.data), BlockUtil.getProperty(blockBelow, FACING).data);
        } else {
            return Blocks.getAirMapped().getDefaultState();
        }
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(PART);
    }

    @Override
    public boolean blockIsAllowed(Block block) {
        return block.data instanceof WRLStationCeilingBlock;
    }
}
