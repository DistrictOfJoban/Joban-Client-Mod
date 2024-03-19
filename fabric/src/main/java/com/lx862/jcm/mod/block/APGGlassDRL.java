package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.entity.APGDoorDRLBlockEntity;
import com.lx862.jcm.mod.block.entity.APGGlassDRLBlockEntity;
import com.lx862.jcm.mod.registry.Items;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.VoxelUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mod.block.BlockAPGGlass;

public class APGGlassDRL extends BlockAPGGlass {
    @Override
    public Item asItem2() {
        return Items.APG_GLASS_DRL.get();
    }

    @Override
    public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new APGGlassDRLBlockEntity(blockPos, blockState);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView blockView, BlockPos pos, ShapeContext shapeContext) {
        int height = BlockUtil.getProperty(state, new Property<>(HALF.data)) == DoubleBlockHalf.UPPER ? 2 : 16;
        return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 0.0, 0.0, 0.0, 16.0, height, 4.0);
    }

    @Override
    public VoxelShape getCollisionShape2(BlockState state, BlockView blockView, BlockPos pos, ShapeContext shapeContext) {
        int height = BlockUtil.getProperty(state, new Property<>(HALF.data)) == DoubleBlockHalf.UPPER ? 9 : 16;
        return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 0.0, 0.0, 0.0, 16.0, height, 4.0);
    }
}
