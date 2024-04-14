package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.DirectionalBlock;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.util.VoxelUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;

import java.util.List;

import static com.lx862.jcm.mod.util.JCMUtil.playerHoldingBrush;

public class LightLanternBlock extends DirectionalBlock {

    public static final BooleanProperty LIT = BlockProperties.POWERED;

    public LightLanternBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return VoxelUtil.getShape16(0, 0, 0, 16, 8, 16);
    }

    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient()) {
            ItemStack heldItem = player.getStackInHand(hand);

            if (playerHoldingBrush(player)) {
                BlockState newState = state.cycle(new Property<>(LIT.data));

                world.setBlockState(pos, newState);
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(LIT);
    }
}
