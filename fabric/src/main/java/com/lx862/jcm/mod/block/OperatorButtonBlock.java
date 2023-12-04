package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.WallAttachedBlock;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import com.lx862.jcm.mod.util.VoxelUtil;
import org.mtr.mapping.holder.*;
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
        return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 5, 5, 0, 11, 11.5, 0.2);
    }

    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        super.onUse2(state, world, pos, player, hand, hit);
        // TODO: Temporary item, replace with Driver Key after adding MTR as dependencies
        if (player.isHolding(Items.getGrassBlockMapped())) {
            return ActionResult.SUCCESS;
        } else {
            player.sendMessage(Text.cast(TextUtil.translatable(TextCategory.HUD, "operator_button.fail").formatted(TextFormatting.RED)), true);
            return ActionResult.FAIL;
        }
    }

    @Override
    public void onServerUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        // TODO: Temporary item, replace with Driver Key after adding MTR as dependencies
        if (player.isHolding(Items.getGrassBlockMapped())) {
            setPowered(world, state, pos, true);
            scheduleBlockTick(world, pos, new Block(this), poweredDuration);
        }
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
        // TODO does this work?
        world.updateNeighbors(pos.offset(blockFacing), new Block(this));
    }
}
