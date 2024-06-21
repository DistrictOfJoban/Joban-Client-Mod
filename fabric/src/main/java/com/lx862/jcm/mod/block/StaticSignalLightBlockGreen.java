package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.entity.StaticSignalLightBlockEntity;
import org.mtr.mapping.holder.BlockSettings;

public class StaticSignalLightBlockGreen extends StaticSignalLightBlock {
    public StaticSignalLightBlockGreen(BlockSettings settings) {
        super(settings, 2, 14);
    }
    @Override
    public StaticSignalLightBlockEntity.SignalType getSignalType() {
        return StaticSignalLightBlockEntity.SignalType.GREEN;
    }
}
