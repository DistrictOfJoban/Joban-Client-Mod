package com.lx862.jcm.blocks;

import com.lx862.jcm.blocks.base.WallAttachedBlock;
import com.lx862.jcm.blocks.data.BlockProperties;
import com.lx862.jcm.util.VoxelUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class OperatorButtonBlock extends WallAttachedBlock {
    private final int poweredDuration;
    public OperatorButtonBlock(Settings settings, int poweredDuration) {
        super(settings);
        setDefaultState(getDefaultState().with(BlockProperties.POWERED, false));
        this.poweredDuration = poweredDuration;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return VoxelUtil.getDirectionalShape16(state.get(FACING),5, 5, 0, 11, 11.5, 0.2);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        // TODO: Temporary item, replace with Driver Key after adding MTR as dependencies
        if(player.isHolding(Items.GRASS_BLOCK)) {
            setPowered(world, state, pos, true);
            world.scheduleBlockTick(pos, this, poweredDuration);

            return ActionResult.SUCCESS;
        } else {
            // TODO: Add message to tell the player!
            return ActionResult.FAIL;
        }
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if(state.get(BlockProperties.POWERED)) {
            setPowered(world, state, pos, false);
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(BlockProperties.POWERED);
    }




    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return true;
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return state.get(BlockProperties.POWERED) ? 15 : 0;
    }

    public int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return state.get(BlockProperties.POWERED) ? 15 : 0;
    }

    private void setPowered(World world, BlockState blockState, BlockPos pos, boolean powered) {
        world.setBlockState(pos, blockState.with(BlockProperties.POWERED, powered));
        updateNearbyBlock(world, pos, blockState.get(FACING));
    }

    private void updateNearbyBlock(World world, BlockPos pos, Direction blockFacing) {
        world.updateNeighborsAlways(pos.offset(blockFacing), this);
    }
}
