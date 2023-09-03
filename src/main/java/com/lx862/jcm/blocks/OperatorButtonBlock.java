package com.lx862.jcm.blocks;

import com.lx862.jcm.blocks.base.WallAttachedBlock;
import com.lx862.jcm.blocks.data.BlockProperties;
import com.lx862.jcm.util.BlockUtil;
import com.lx862.jcm.util.VoxelUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.holder.BooleanProperty;
import org.mtr.mapping.tool.HolderBase;

import java.util.List;

public class OperatorButtonBlock extends WallAttachedBlock {
    private final int poweredDuration;
    public static final BooleanProperty POWERED = BlockProperties.POWERED;
    public OperatorButtonBlock(BlockSettings settings, int poweredDuration) {
        super(settings);
        setDefaultState2(getDefaultState2().with(new Property<>(POWERED.data), false));
        this.poweredDuration = poweredDuration;
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return VoxelUtil.getDirectionalShape16(BlockUtil.getStateProperty(state, FACING).data,5, 5, 0, 11, 11.5, 0.2);
    }

    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        // TODO: Temporary item, replace with Driver Key after adding MTR as dependencies
        if(player.isHolding(Items.getGrassBlockMapped())) {
            world.setBlockState(pos, state.with(new Property<>(POWERED.data), true));
            updateNearbyBlock(world, pos, BlockUtil.getStateProperty(state, FACING));
            world.scheduleBlockTick(pos, new Block(this), poweredDuration);

            return ActionResult.SUCCESS;
        } else {
            // TODO: Add message to tell the player!
            return ActionResult.FAIL;
        }
    }

    @Override
    public void scheduledTick2(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if(state.get(new Property<>(POWERED.data))) {
            setPowered(world, state, pos, false);
        }
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(POWERED);
    }


    @Override
    public boolean emitsRedstonePower2(BlockState state) {
        return true;
    }

    @Override
    public int getWeakRedstonePower2(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return BlockUtil.getStateProperty(state, POWERED) ? 15 : 0;
    }

    @Override
    public int getStrongRedstonePower2(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return BlockUtil.getStateProperty(state, POWERED) ? 15 : 0;
    }

    private void setPowered(ServerWorld world, BlockState blockState, BlockPos pos, boolean powered) {
        world.setBlockState(pos, blockState.with(new Property<>(POWERED.data), powered));
        updateNearbyBlock(world, pos, Direction.convert(blockState.get(new Property<>(FACING.data))));
    }

    private void updateNearbyBlock(ServerWorld world, BlockPos pos, Direction blockFacing) {
        world.updateNeighborsAlways(pos.offset(blockFacing), new Block(this));
    }

    private void updateNearbyBlock(World world, BlockPos pos, Direction blockFacing) {
        world.updateNeighborsAlways(pos.offset(blockFacing), new Block(this));
    }
}
