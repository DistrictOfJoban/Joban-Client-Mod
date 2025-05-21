package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.entity.StationNameStandingBlockEntity;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mod.block.BlockStationNameTallStanding;

/**
 * The old station name standing block. Block is moved to MTR Mod, now this is just a block wrapping the mtr mod variant to retain the Block Entity.
 */
@Deprecated
public class StationNameStandingBlock extends BlockStationNameTallStanding {
    public StationNameStandingBlock() {
        super();
    }

    @Override
    public Item asItem2() {
        // Redirect to official MTR block
        return org.mtr.mod.Blocks.STATION_NAME_TALL_STANDING.get().asItem();
    }

    @Override
    public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new StationNameStandingBlockEntity(blockPos, blockState);
    }
}
