package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.entity.VerticalPIDSBlockEntity;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mod.Blocks;
import org.mtr.mod.block.IBlock;
import com.lx862.jcm.mod.block.base.Vertical2Block;
import org.mtr.mapping.mapper.BlockWithEntity;
import com.lx862.jcm.mod.network.gui.PIDSGUIPacket;
import com.lx862.jcm.mod.registry.Networking;

public class VerticalPIDSBlock extends Vertical2Block implements BlockWithEntity {
    public VerticalPIDSBlock() {
        super(Blocks.createDefaultBlockSettings(true, (state) -> 5).nonOpaque());
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
       return IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 16, 1, IBlock.getStatePropertySafe(state, FACING));
    }

    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        return IBlock.checkHoldingBrush(world, player, () -> {
            VerticalPIDSBlockEntity be = (VerticalPIDSBlockEntity) world.getBlockEntity(pos).data;
            Networking.sendPacketToClient(player, new PIDSGUIPacket(pos, be.getCustomMessages(), be.getRowHidden(), be.platformNumberHidden(), be.getPresetId()));
        });
    }

    @Override
    public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new VerticalPIDSBlockEntity(blockPos, blockState);
    }
}