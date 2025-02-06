package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.DirectionalBlock;
import com.lx862.jcm.mod.registry.Blocks;
import org.mtr.mapping.holder.*;
import org.mtr.mod.block.IBlock;

public class CircleWallBlock extends DirectionalBlock {
    public CircleWallBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        BlockState superState = super.getPlacementState2(ctx);

        if(superState != null && ctx.getPlayer() != null && autoPlace(ctx.getWorld(), ctx.getBlockPos(), superState, ctx.getPlayerFacing())) {
            ctx.getPlayer().swingHand(ctx.getHand(), !ctx.getWorld().isClient());
            return null;
        } else {
            return superState;
        }
    }

    // Why does light still pass through aaaaa
    @Override
    public boolean isTranslucent2(BlockState state, BlockView world, BlockPos pos) {
        return false;
    }

    @Override
    public VoxelShape getCullingShape2(BlockState state, BlockView world, BlockPos pos) {
        return IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 16, 15, IBlock.getStatePropertySafe(state, FACING));
    }

    /**
     * Automatically place tunnel walls in a shifted block position to provide easier placement
     * @return True if the block is successfully placed in the shifted position, False otherwise
     */
    private boolean autoPlace(World world, BlockPos pos, BlockState state, Direction playerFacing) {
        Block thisBlock = this.asBlock2();
        Block blockBelow = world.getBlockState(pos.down()).getBlock();
        BlockPos shiftedBlockPos = null;

        if(blockBelow.equals(Blocks.CIRCLE_WALL_1.get()) && thisBlock.equals(Blocks.CIRCLE_WALL_2.get())) {
            shiftedBlockPos = pos.offset(playerFacing);
        }

        if(blockBelow.equals(Blocks.CIRCLE_WALL_4.get()) && thisBlock.equals(Blocks.CIRCLE_WALL_5.get()) ||
           blockBelow.equals(Blocks.CIRCLE_WALL_5.get()) && thisBlock.equals(Blocks.CIRCLE_WALL_6.get())
        ) {
            shiftedBlockPos = pos.offset(playerFacing.getOpposite());
        }

        if(shiftedBlockPos != null && world.getBlockState(shiftedBlockPos).isAir()) {
            world.setBlockState(shiftedBlockPos, state);
            return true;
        } else {
            return false;
        }
    }
}
