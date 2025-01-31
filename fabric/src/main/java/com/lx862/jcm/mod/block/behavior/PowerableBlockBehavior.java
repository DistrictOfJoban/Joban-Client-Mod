package com.lx862.jcm.mod.block.behavior;

import com.lx862.jcm.mod.data.BlockProperties;
import org.mtr.mapping.holder.*;
import org.mtr.mod.block.IBlock;

public interface PowerableBlockBehavior  {
    BooleanProperty UNPOWERED = BlockProperties.UNPOWERED;

    default void updateRedstone(World world, BlockPos pos, Block block, BlockState state) {
        world.updateNeighbors(pos, block);
        world.updateNeighbors(pos.offset(IBlock.getStatePropertySafe(state, BlockProperties.FACING)), block);
    }

    default void updateAllRedstone(World world, BlockPos pos, Block block, BlockState state) {
        world.updateNeighbors(pos, block);

        for(Direction direction : Direction.values()) {
            world.updateNeighbors(pos.offset(direction), block);
        }
    }
}
