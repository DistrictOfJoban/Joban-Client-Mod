package com.lx862.jcm.mod.block.base;

import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.block.IBlock;

import java.util.List;

public abstract class Vertical3Block extends DirectionalBlock {
    private static final int HEIGHT = 3;
    public static final EnumProperty<IBlock.EnumThird> THIRD = BlockProperties.VERTICAL_PART_3;

    public Vertical3Block(BlockSettings settings) {
        super(settings);
    }

    @Override
    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        return BlockUtil.isReplacable(ctx.getWorld(), ctx.getBlockPos(), Direction.UP, ctx, HEIGHT) ? super.getPlacementState2(ctx) : null;
    }

    @Override
    public void onPlaced2(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        world.setBlockState(pos.up(1), state.with(new Property<>(THIRD.data), IBlock.EnumThird.MIDDLE));
        world.setBlockState(pos.up(2), state.with(new Property<>(THIRD.data), IBlock.EnumThird.UPPER));
    }

    @Override
    public BlockState getStateForNeighborUpdate2(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if(!BlockUtil.canSurvive(state.getBlock(), world, pos, Direction.UP, getPart(IBlock.getStatePropertySafe(state, new Property<>(THIRD.data))), HEIGHT)) {
            return Blocks.getAirMapped().getDefaultState();
        }

        return super.getStateForNeighborUpdate2(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(THIRD);
    }

    @Override
    public BlockPos[] getAllPos(BlockState state, World world, BlockPos pos) {
        switch(IBlock.getStatePropertySafe(state, new Property<>(THIRD.data))) {
            case LOWER:
                return new BlockPos[]{
                        pos,
                        pos.up(),
                        pos.up(2)};
            case MIDDLE:
                return new BlockPos[]{
                        pos.down(),
                        pos,
                        pos.up()};
            case UPPER:
                return new BlockPos[]{
                        pos.down(2),
                        pos.down(),
                        pos};
            default:
                return super.getAllPos(state, world, pos);
        }
    }

    public int getPart(IBlock.EnumThird e) {
        if(e == IBlock.EnumThird.LOWER) {
            return 0;
        } else if(e == IBlock.EnumThird.MIDDLE) {
            return 1;
        } else {
            return 2;
        }
    }
}
