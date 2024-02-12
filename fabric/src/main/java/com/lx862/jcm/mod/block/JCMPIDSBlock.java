package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.Horizontal2MirroredBlock;
import org.mtr.mapping.holder.BlockSettings;
import org.mtr.mapping.mapper.BlockWithEntity;

public abstract class JCMPIDSBlock extends Horizontal2MirroredBlock implements BlockWithEntity {
    public JCMPIDSBlock(BlockSettings settings) {
        super(settings);
    }
}
