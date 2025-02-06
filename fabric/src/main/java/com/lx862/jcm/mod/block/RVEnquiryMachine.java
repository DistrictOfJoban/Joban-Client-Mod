package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.Vertical2Block;
import com.lx862.jcm.mod.block.behavior.EnquiryMachineBehavior;
import com.lx862.jcm.mod.data.EnquiryScreenType;
import org.mtr.mapping.holder.*;
import org.mtr.mod.block.IBlock;

public class RVEnquiryMachine extends Vertical2Block implements EnquiryMachineBehavior {
    public RVEnquiryMachine(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        switch(IBlock.getStatePropertySafe(state, new Property<>(HALF.data))) {
            case LOWER:
                return IBlock.getVoxelShapeByDirection(3, 0, 2, 13, 16, 14, IBlock.getStatePropertySafe(state, FACING));
            case UPPER:
                return IBlock.getVoxelShapeByDirection(3, 0, 2, 13, 12, 14, IBlock.getStatePropertySafe(state, FACING));
            default:
                return VoxelShapes.empty();
        }
    }

    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if(!world.isClient()) enquiry(EnquiryScreenType.NONE, pos, world, player);
        return ActionResult.SUCCESS;
    }
}
