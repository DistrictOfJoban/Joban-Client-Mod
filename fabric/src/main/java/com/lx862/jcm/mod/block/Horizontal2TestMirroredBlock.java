package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.Horizontal2MirroredBlock;
import org.mtr.mapping.holder.*;

public class Horizontal2TestMirroredBlock extends Horizontal2MirroredBlock {

    public Horizontal2TestMirroredBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return VoxelShapes.fullCube();
    }
}