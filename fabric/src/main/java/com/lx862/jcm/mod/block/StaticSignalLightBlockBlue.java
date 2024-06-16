package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.entity.StaticSignalLightBlockEntity;
import org.mtr.mapping.holder.BlockSettings;

public class StaticSignalLightBlockBlue extends StaticSignalLightBlock {
    public StaticSignalLightBlockBlue(BlockSettings settings) {
        super(settings, 2, 14);
    }

    @Override
    public StaticSignalLightBlockEntity.SignalType getSignalType() {
        return StaticSignalLightBlockEntity.SignalType.BLUE;
    }
}
