package com.lx862.jcm.mod.blocks;

import com.lx862.jcm.mod.blocks.base.WallAttachedBlock;
import com.lx862.jcm.mod.blocks.behavior.EnquiryMachine;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.VoxelUtil;
import org.mtr.mapping.holder.*;

public class MTREnquiryMachineWall extends WallAttachedBlock implements EnquiryMachine {
    public MTREnquiryMachineWall(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 2.5, 1, 0, 13.5, 15, 0.2);
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
