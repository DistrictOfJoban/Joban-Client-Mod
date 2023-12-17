package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.Horizontal2MirroredBlock;
import com.lx862.jcm.mod.block.entity.RVPIDSBlockEntity;
import com.lx862.jcm.mod.network.gui.PIDSGUIPacket;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.VoxelUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockWithEntity;

public class RVPIDSBlock extends Horizontal2MirroredBlock implements BlockWithEntity {

    public RVPIDSBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        VoxelShape vx1 = VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 6, -3, 0, 10, 11, 12);
        VoxelShape vx2 = VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 7.5, 0, 8.5, 8.5, 16, 9.5);
        return VoxelShapes.union(vx1, vx2);
    }

    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        super.onUse2(state, world, pos, player, hand, hit);
        return ActionResult.SUCCESS;
    }

    @Override
    public void onServerUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        RVPIDSBlockEntity thisEntity = (RVPIDSBlockEntity) world.getBlockEntity(pos).data;

        // TODO: Temporary item, replace with Driver Key after adding MTR as dependencies
        if (player.isHolding(Items.getGrassBlockMapped())) {
            Networking.sendPacketToClient(player, new PIDSGUIPacket(pos, thisEntity.getCustomMessages(), thisEntity.getHidePlatforms(), thisEntity.getHidePlatformNumber(), thisEntity.getPresetId()));
        }
    }

    @Override
    public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new RVPIDSBlockEntity(blockPos, blockState);
    }
}