package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.StaticSignalLightBlock;
import com.lx862.jcm.mod.block.entity.StaticSignalLightBlockEntity;
import org.mtr.mapping.holder.BlockSettings;

public class StaticSignalLightBlockRedBelow extends StaticSignalLightBlock {
    public StaticSignalLightBlockRedBelow(BlockSettings settings) {
        super(settings);
    }

    @Override
    public StaticSignalLightBlockEntity.SignalType getSignalType() {
        return StaticSignalLightBlockEntity.SignalType.RED_BELOW;
    }
}
