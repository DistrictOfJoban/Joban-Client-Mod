package com.lx862.jcm.mod.blocks;

import com.lx862.jcm.mod.blocks.base.Vertical2Block;
import com.lx862.jcm.mod.blocks.behavior.EnquiryMachine;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.VoxelUtil;
import org.mtr.mapping.holder.*;

public class RVEnquiryMachine extends Vertical2Block implements EnquiryMachine {
    public RVEnquiryMachine(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        switch(BlockUtil.getProperty(state, PART)) {
            case 0:
                return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 3, 0, 2, 13, 16, 14);
            case 1:
                return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 3, 0, 2, 13, 12, 14);
            default:
                return VoxelShapes.empty();
        }
    }

    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        super.onUse2(state, world, pos, player, hand, hit);
        return ActionResult.SUCCESS;
    }

    @Override
    public void onServerUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        tapEnquiryMachine(world, player);
    }
}
