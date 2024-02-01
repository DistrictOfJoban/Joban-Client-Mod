package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.StaticSignalLightBlock;
import com.lx862.jcm.mod.block.entity.StaticSignalLightBlockEntity;
import org.mtr.mapping.holder.BlockSettings;

public class StaticSignalLightBlockBlue extends StaticSignalLightBlock {
    public StaticSignalLightBlockBlue(BlockSettings settings) {
        super(settings);
    }

    @Override
    public StaticSignalLightBlockEntity.SignalType getSignalType() {
        return StaticSignalLightBlockEntity.SignalType.BLUE;
    }
}
