package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.WallAttachedBlock;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.util.*;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.Items;
import org.mtr.mod.block.IBlock;

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
        return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 5, 5, 0, 11, 11.5, 0.2);
    }

    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if(world.isClient()) return ActionResult.SUCCESS;

        return IBlock.checkHoldingItem(world, player, (item) -> {
            setPowered(world, state, pos, true);
            scheduleBlockTick(world, pos, new Block(this), poweredDuration);
        }, () -> {
            player.sendMessage(Text.cast(TextUtil.translatable(TextCategory.HUD, "operator_button.fail").formatted(TextFormatting.RED)), true);
        }, Items.DRIVER_KEY.get());
    }

    @Override
    public void scheduledTick2(BlockState state, ServerWorld serverWorld, BlockPos pos, Random random) {
        if (state.get(new Property<>(POWERED.data))) {
            setPowered(World.cast(serverWorld), state, pos, false);
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
        return BlockUtil.getProperty(state, POWERED) ? 15 : 0;
    }

    @Override
    public int getStrongRedstonePower2(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return BlockUtil.getProperty(state, POWERED) ? 15 : 0;
    }

    private void setPowered(World world, BlockState blockState, BlockPos pos, boolean powered) {
        world.setBlockState(pos, blockState.with(new Property<>(POWERED.data), powered));
        updateNearbyBlock(world, pos, Direction.convert(blockState.get(new Property<>(FACING.data))));
    }

    private void updateNearbyBlock(World world, BlockPos pos, Direction blockFacing) {
        world.updateNeighbors(pos.offset(blockFacing), new Block(this));
    }
}
