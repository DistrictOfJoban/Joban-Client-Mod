package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.VerticallyAttachedBlock;
import com.lx862.jcm.mod.network.gui.DemoScreenPacket;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.Utils;
import com.lx862.jcm.mod.util.VoxelUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.registry.Registry;

public class SpotLampBlock extends VerticallyAttachedBlock {

    public SpotLampBlock(BlockSettings settings) {
        super(settings, true, true);
    }

    // TODO: FOR PREVIEW ONLY, REMOVE
    @Override
    public void onServerUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if(Utils.playerHoldingBrush(player)) {
            Registry.sendPacketToClient(ServerPlayerEntity.cast(player), new DemoScreenPacket());
        }
    }

        @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        if (BlockUtil.getProperty(state, TOP)) {
            return VoxelUtil.getShape16(4, 15.75, 4, 12, 16, 12);
        } else {
            return VoxelUtil.getShape16(4, 0, 4, 12, 0.25, 12);
        }
    }
}
