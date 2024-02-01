package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.StaticSignalLightBlock;
import com.lx862.jcm.mod.block.entity.StaticSignalLightBlockEntity;
import org.mtr.mapping.holder.*;

public class StaticSignalLightBlockGreen extends StaticSignalLightBlock {
    public StaticSignalLightBlockGreen(BlockSettings settings) {
        super(settings);
    }
    @Override
    public StaticSignalLightBlockEntity.SignalType getSignalType() {
        return StaticSignalLightBlockEntity.SignalType.GREEN;
    }
}
