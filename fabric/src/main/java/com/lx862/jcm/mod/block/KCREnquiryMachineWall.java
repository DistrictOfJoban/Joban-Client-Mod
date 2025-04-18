package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.WallAttachedBlock;
import com.lx862.jcm.mod.block.behavior.EnquiryMachineBehavior;
import com.lx862.jcm.mod.data.EnquiryScreenType;
import org.mtr.mapping.holder.*;
import org.mtr.mod.block.IBlock;

public class KCREnquiryMachineWall extends WallAttachedBlock implements EnquiryMachineBehavior {
    public KCREnquiryMachineWall(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return IBlock.getVoxelShapeByDirection(3.5, 2.5, 0, 12.5, 13.5, 1, IBlock.getStatePropertySafe(state, FACING));
    }

    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if(!world.isClient()) enquiry(EnquiryScreenType.NONE, pos, world, player);
        return ActionResult.SUCCESS;
    }
}
