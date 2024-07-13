package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.Vertical3Block;
import com.lx862.jcm.mod.block.behavior.PowerableBlockBehavior;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.block.IBlock;

import java.util.List;

public class TMLEmergencyButtonBlock extends Vertical3Block implements PowerableBlockBehavior {
    public TMLEmergencyButtonBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        switch (IBlock.getStatePropertySafe(state, new Property<>(THIRD.data))) {
            case LOWER:
            case MIDDLE:
                return IBlock.getVoxelShapeByDirection(4, 0, 7.5, 12, 16, 8.5, IBlock.getStatePropertySafe(state, FACING));
            case UPPER:
                return IBlock.getVoxelShapeByDirection(4, 0, 7.5, 12, 12, 8.5, IBlock.getStatePropertySafe(state, FACING));
            default:
                return VoxelShapes.empty();
        }
    }

    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        for(BlockPos bPos : getAllPos(state, world, pos)) {
            BlockState bs = world.getBlockState(bPos);
            world.setBlockState(bPos, bs.with(new Property<>(UNPOWERED.data), false));
            updateAllRedstone(World.cast(world), bPos, new Block(this), bs);
            scheduleBlockTick(world, bPos, new Block(this), 20);
        }
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