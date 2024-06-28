package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.Vertical2Block;
import com.lx862.jcm.mod.block.behavior.EnquiryMachineBehavior;
import com.lx862.jcm.mod.data.EnquiryScreenType;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.VoxelUtil;
import org.mtr.mapping.holder.*;

public class RVEnquiryMachine extends Vertical2Block implements EnquiryMachineBehavior {
    public RVEnquiryMachine(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        switch(BlockUtil.getProperty(state, new Property<>(HALF.data))) {
            case LOWER:
                return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 3, 0, 2, 13, 16, 14);
            case UPPER:
                return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 3, 0, 2, 13, 12, 14);
            default:
                return VoxelShapes.empty();
        }
    }

    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if(!world.isClient()) enquiry(EnquiryScreenType.RV, world, player);;
        return ActionResult.SUCCESS;
    }
}
