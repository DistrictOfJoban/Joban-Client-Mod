package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.entity.APGGlassDRLBlockEntity;
import com.lx862.jcm.mod.registry.Items;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mod.block.BlockAPGGlass;
import org.mtr.mod.block.IBlock;

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
        int height = IBlock.getStatePropertySafe(state, new Property<>(HALF.data)) == DoubleBlockHalf.UPPER ? 2 : 16;
        return IBlock.getVoxelShapeByDirection(0.0, 0.0, 0.0, 16.0, height, 4.0, IBlock.getStatePropertySafe(state, FACING));
    }

    @Override
    public VoxelShape getCollisionShape2(BlockState state, BlockView blockView, BlockPos pos, ShapeContext shapeContext) {
        int height = IBlock.getStatePropertySafe(state, new Property<>(HALF.data)) == DoubleBlockHalf.UPPER ? 9 : 16;
        return IBlock.getVoxelShapeByDirection(0.0, 0.0, 0.0, 16.0, height, 4.0, IBlock.getStatePropertySafe(state, FACING));
    }
}
