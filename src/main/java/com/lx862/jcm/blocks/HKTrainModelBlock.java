package com.lx862.jcm.blocks;

import com.lx862.jcm.blocks.base.HorizontalMultiBlock;
import com.lx862.jcm.util.VoxelUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

public class HKTrainModelBlock extends HorizontalMultiBlock {

    public HKTrainModelBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return VoxelUtil.getDirectionalShape16(state.get(FACING), 0, 0, 3.5, 16, 9, 12.5);
    }
}
