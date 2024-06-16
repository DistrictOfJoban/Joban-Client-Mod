package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.entity.StaticSignalLightBlockEntity;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mod.block.BlockSignalLightBase;

public abstract class StaticSignalLightBlock extends BlockSignalLightBase {
    public StaticSignalLightBlock(BlockSettings settings, int shapeX, int shapeHeight) {
        super(settings, shapeX, shapeHeight);
    }

    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        return ActionResult.PASS;
    }

    public abstract StaticSignalLightBlockEntity.SignalType getSignalType();

    @Override
    public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new StaticSignalLightBlockEntity(getSignalType(), blockPos, blockState);
    }
}
