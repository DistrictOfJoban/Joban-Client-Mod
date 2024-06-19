package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.VerticallyAttachedBlock;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.VoxelUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.block.IBlock;

import java.util.List;

import static com.lx862.jcm.mod.util.JCMUtil.playerHoldingBrush;

public class SpotLampBlock extends VerticallyAttachedBlock {
    public static final BooleanProperty LIT = BlockProperties.POWERED;

    public SpotLampBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        if (BlockUtil.getProperty(state, TOP)) {
            return VoxelUtil.getShape16(4, 15.75, 4, 12, 16, 12);
        } else {
            return VoxelUtil.getShape16(4, 0, 4, 12, 0.25, 12);
        }
    }

    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        return IBlock.checkHoldingBrush(world, player, () -> {
            BlockState newState = state.cycle(new Property<>(LIT.data));
            world.setBlockState(pos, newState);
        });
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(LIT);
    }
}
