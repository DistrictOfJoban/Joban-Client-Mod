package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.Vertical3Block;
import com.lx862.jcm.mod.block.behavior.PowerableBlockBehavior;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.block.IBlock;

import java.util.List;

public class HelpLineStandingTKLBlock extends Vertical3Block implements PowerableBlockBehavior {
    public HelpLineStandingTKLBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        switch (IBlock.getStatePropertySafe(state, new Property<>(THIRD.data))) {
            case LOWER:
                return IBlock.getVoxelShapeByDirection(4, 0, 7.5, 12, 16, 8.5, IBlock.getStatePropertySafe(state, FACING));
            case UPPER:
                return IBlock.getVoxelShapeByDirection(4, 0, 7.5, 12, 20, 8.5, IBlock.getStatePropertySafe(state, FACING));
            case MIDDLE:
                VoxelShape vx1 = IBlock.getVoxelShapeByDirection(4, 0, 7.5, 12, 16, 8.5, IBlock.getStatePropertySafe(state, FACING));
                VoxelShape vx2 = IBlock.getVoxelShapeByDirection(5.5, 0, 8.5, 10.5, 15, 10.5, IBlock.getStatePropertySafe(state, FACING));
                return VoxelShapes.union(vx1, vx2);
            default:
                return VoxelShapes.empty();
        }
    }

    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        for(BlockPos bPos : getAllPos(state, world, pos)) {
            BlockState blockState = world.getBlockState(bPos);
            world.setBlockState(bPos, blockState.with(new Property<>(UNPOWERED.data), false));
            updateAllRedstone(World.cast(world), bPos, new Block(this), blockState);
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