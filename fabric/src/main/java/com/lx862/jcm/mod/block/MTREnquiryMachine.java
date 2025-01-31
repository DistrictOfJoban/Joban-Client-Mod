package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.Vertical2Block;
import com.lx862.jcm.mod.block.behavior.EnquiryMachineBehavior;
import com.lx862.jcm.mod.data.EnquiryScreenType;
import org.mtr.mod.block.IBlock;
import org.mtr.mapping.holder.*;

public class MTREnquiryMachine extends Vertical2Block implements EnquiryMachineBehavior {
    public MTREnquiryMachine(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        switch(IBlock.getStatePropertySafe(state, new Property<>(HALF.data))) {
            case LOWER:
                return IBlock.getVoxelShapeByDirection(4, 0, 4.5, 12, 15, 11.5, IBlock.getStatePropertySafe(state, FACING));
            case UPPER:
                VoxelShape vx1 = IBlock.getVoxelShapeByDirection(4, 0, 4.5, 12, 5.6, 11.5, IBlock.getStatePropertySafe(state, FACING));
                VoxelShape vx2 = IBlock.getVoxelShapeByDirection(4, 5.6569, 5.85, 12, 10.1519, 5.95, IBlock.getStatePropertySafe(state, FACING));
                return VoxelShapes.union(vx1, vx2);
            default:
                return VoxelShapes.empty();
        }
    }

    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if(!world.isClient()) enquiry(EnquiryScreenType.CLASSIC, pos, world, player);
        return ActionResult.SUCCESS;
    }
}
