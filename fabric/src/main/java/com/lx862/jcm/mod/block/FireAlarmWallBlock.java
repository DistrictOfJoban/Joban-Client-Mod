package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.WallAttachedBlock;
import com.lx862.jcm.mod.block.behavior.PowerableBlockBehavior;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.block.IBlock;

import java.util.List;

public class FireAlarmWallBlock extends WallAttachedBlock implements PowerableBlockBehavior {
    public FireAlarmWallBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return IBlock.getVoxelShapeByDirection(5, 5, 0, 11, 11, 1, IBlock.getStatePropertySafe(state, FACING));
    }

    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        world.setBlockState(pos, state.with(new Property<>(UNPOWERED.data), false));
        updateAllRedstone(World.cast(world), pos, new Block(this), state);
        scheduleBlockTick(world, pos, new Block(this), 20);
        return ActionResult.SUCCESS;
    }

    @Override
    public void scheduledTick2(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        world.setBlockState(pos, state.with(new Property<>(UNPOWERED.data), true));
        updateAllRedstone(World.cast(world), pos, new Block(this), state);
    }

    @Override
    public int getWeakRedstonePower2(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return IBlock.getStatePropertySafe(state, UNPOWERED) ? 0 : 15;
    }

    public int getStrongRedstonePower2(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return IBlock.getStatePropertySafe(state, UNPOWERED) ? 0 : 15;
    }

    @Override
    public boolean emitsRedstonePower2(BlockState state) {
        return true;
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(UNPOWERED);
    }
}
