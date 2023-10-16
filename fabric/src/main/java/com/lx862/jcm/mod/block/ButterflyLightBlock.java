package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.DirectionalBlock;
import com.lx862.jcm.mod.block.entity.ButterflyLightBlockEntity;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.network.gui.ButterflyLightGUIPacket;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.Utils;
import com.lx862.jcm.mod.util.VoxelUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockWithEntity;
import org.mtr.mapping.tool.HolderBase;

import java.util.List;

public class ButterflyLightBlock extends DirectionalBlock implements BlockWithEntity {
    public static final BooleanProperty LIT = BlockProperties.LIT;

    public ButterflyLightBlock(BlockSettings settings) {
        super(settings);
        setDefaultState2(getDefaultState2().with(new Property<>(LIT.data), false));
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 2, 0, 0, 14, 5.85, 10);
    }

    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        super.onUse2(state, world, pos, player, hand, hit);
        return getBrushActionResult(player);
    }

    @Override
    public void onServerUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ButterflyLightBlockEntity thisEntity = (ButterflyLightBlockEntity)world.getBlockEntity(pos).data;

        if(Utils.playerHoldingBrush(player)) {
            Networking.sendPacketToClient(player, new ButterflyLightGUIPacket(pos, thisEntity.getSecondsToBlink()));
        }
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(LIT);
    }

    @Override
    public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new ButterflyLightBlockEntity(blockPos, blockState);
    }
}
