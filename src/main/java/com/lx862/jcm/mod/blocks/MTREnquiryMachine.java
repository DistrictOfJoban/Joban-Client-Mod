package com.lx862.jcm.mod.blocks;

import com.lx862.jcm.mod.blocks.base.Vertical2Block;
import com.lx862.jcm.mod.blocks.behavior.EnquiryMachine;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.VoxelUtil;
import org.mtr.mapping.holder.*;

public class MTREnquiryMachine extends Vertical2Block implements EnquiryMachine {
    public MTREnquiryMachine(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        switch(BlockUtil.getProperty(state, PART)) {
            case 0:
                return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 4, 0, 4.5, 12, 15, 11.5);
            case 1:
                VoxelShape vx1 = VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 4, 0, 4.5, 12, 5.6, 11.5);
                VoxelShape vx2 = VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 4, 5.6569, 5.85, 12, 10.1519, 5.95);
                return VoxelShapes.union(vx1, vx2);
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
