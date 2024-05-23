package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.Vertical2Block;
import com.lx862.jcm.mod.block.behavior.EnquiryMachineBehavior;
import com.lx862.jcm.mod.data.EnquiryLog;
import com.lx862.jcm.mod.data.Entry;
import com.lx862.jcm.mod.network.gui.EnquiryUpdateGUIPacket;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.VoxelUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mod.Init;

import java.util.List;

public class MTREnquiryMachine extends Vertical2Block implements EnquiryMachineBehavior {
    public MTREnquiryMachine(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        switch(BlockUtil.getProperty(state, new Property<>(HALF.data))) {
            case LOWER:
                return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 4, 0, 4.5, 12, 15, 11.5);
            case UPPER:
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
        enquiry(world, player);
        List<Entry> entries = EnquiryLog.getEntries(player, player.getUuidAsString());
        Networking.sendPacketToClient(player, new EnquiryUpdateGUIPacket(entries));
    }
}
