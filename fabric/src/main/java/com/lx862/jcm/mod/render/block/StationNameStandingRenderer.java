package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.block.entity.StationNameStandingBlockEntity;
import org.mtr.mod.block.BlockStationNameTallStanding;
import org.mtr.mod.render.*;

public class StationNameStandingRenderer extends RenderStationNameTall<StationNameStandingBlockEntity> {
    public StationNameStandingRenderer(Argument dispatcher) {
        super(dispatcher, BlockStationNameTallStanding.WIDTH, BlockStationNameTallStanding.HEIGHT, BlockStationNameTallStanding.OFFSET_Y);
    }
}
