package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.entity.APGDoorDRLBlockEntity;
import com.lx862.jcm.mod.registry.Items;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.VoxelUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockWithEntity;
import org.mtr.mod.block.BlockPSDAPGDoorBase;

public class APGDoorDRL extends BlockPSDAPGDoorBase implements BlockWithEntity {
    @Override
    public Item asItem2() {
        return Items.APG_DOOR_DRL.get();
    }

    @Override
    public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new APGDoorDRLBlockEntity(blockPos, blockState);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView blockView, BlockPos pos, ShapeContext shapeContext) {
        int height = BlockUtil.getProperty(state, new Property<>(HALF.data)) == DoubleBlockHalf.UPPER ? 2 : 16;
        return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 0.0, 0.0, 0.0, 16.0, height, 4.0);
    }

//    @Override
//    public VoxelShape getCollisionShape2(BlockState state, BlockView blockView, BlockPos pos, ShapeContext shapeContext) {
//        final org.mtr.mapping.holder.BlockEntity blockEntity = blockView.getBlockEntity(pos);
//        final World world = blockEntity == null ? null : blockEntity.getWorld();
//        if(world == null || !world.isClient()) return VoxelShapes.empty();
//        if(((BlockEntityBase) blockEntity.data).getDoorValue())
//
//        int height = BlockUtil.getProperty(state, new Property<>(HALF.data)) == DoubleBlockHalf.UPPER ? 9 : 16;
//        return VoxelUtil.getDirectionalShape16( BlockUtil.getProperty(state, FACING), 0.0, 0.0, 0.0, 16.0, height, 4.0);
//    }
}
