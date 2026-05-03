package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.WallAttachedBlock;
import com.lx862.jcm.mod.block.behavior.PowerableBlockBehavior;
import com.lx862.jcm.mod.block.entity.TMLEmergencyStopButtonWallBlockEntity;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockWithEntity;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.block.IBlock;

import java.util.List;

public class TMLEmergencyButtonWallBlock extends WallAttachedBlock implements BlockWithEntity, PowerableBlockBehavior {
    public TMLEmergencyButtonWallBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        VoxelShape vx1 = IBlock.getVoxelShapeByDirection(5.25, 0, 0, 10.75, 16, 1, IBlock.getStatePropertySafe(state, FACING));
        VoxelShape vx2 = IBlock.getVoxelShapeByDirection(5.5, 5.75, 0, 10.5, 10.75, 3, IBlock.getStatePropertySafe(state, FACING));
        return VoxelShapes.union(vx1, vx2);
    }

    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        world.setBlockState(pos, state.with(new Property<>(UNPOWERED.data), false));
        updateRedstone(World.cast(world), pos, new Block(this), state);
        scheduleBlockTick(world, pos, new Block(this), 20);
        return ActionResult.SUCCESS;
    }

    @Override
    public BlockRenderType getRenderType2(BlockState state) {
        return BlockRenderType.getEntityblockAnimatedMapped();
    }

    @Override
    public void scheduledTick2(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        world.setBlockState(pos, state.with(new Property<>(UNPOWERED.data), true));
        updateRedstone(World.cast(world), pos, new Block(this), state);
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

    @Override
    public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new TMLEmergencyStopButtonWallBlockEntity(blockPos, blockState);
    }
}