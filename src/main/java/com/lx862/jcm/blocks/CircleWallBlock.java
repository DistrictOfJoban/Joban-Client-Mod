package com.lx862.jcm.blocks;

import com.lx862.jcm.blocks.base.DirectionalBlock;
import com.lx862.jcm.registry.BlockRegistry;
import com.lx862.jcm.util.BlockUtil;
import org.mtr.mapping.holder.*;

public class CircleWallBlock extends DirectionalBlock {
    public CircleWallBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        BlockState superState = super.getPlacementState2(ctx);

        if(autoPlace(ctx, ctx.getWorld(), ctx.getBlockPos(), superState, ctx.getPlayerFacing())) {
            return null;
        } else {
            return superState;
        }
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return VoxelShapes.fullCube();
    }

    /**
     * Automatically place tunnel walls in a shifted block position to provide easier placement
     * @return True if the block is successfully placed in the shifted position, False otherwise
     */
    private boolean autoPlace(ItemPlacementContext ctx, World world, BlockPos pos, BlockState state, Direction playerFacing) {
        PlayerEntity player = ctx.getPlayer();
        if(player == null) return false;

        Block thisBlock = this.asBlock2();
        Block blockBelow = world.getBlockState(pos.down()).getBlock();
        BlockPos shiftedBlockPos = null;

        if(blockBelow.equals(BlockRegistry.CIRCLE_WALL_1.get()) && thisBlock.equals(BlockRegistry.CIRCLE_WALL_2.get())) {
            shiftedBlockPos = pos.offset(playerFacing);
        }

        if(blockBelow.equals(BlockRegistry.CIRCLE_WALL_4.get()) && thisBlock.equals(BlockRegistry.CIRCLE_WALL_5.get()) ||
           blockBelow.equals(BlockRegistry.CIRCLE_WALL_5.get()) && thisBlock.equals(BlockRegistry.CIRCLE_WALL_6.get())
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
