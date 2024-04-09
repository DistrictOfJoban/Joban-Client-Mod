package com.lx862.jcm.mod.block;

import org.mtr.mapping.holder.BlockSettings;
import org.mtr.mapping.mapper.DoorBlockExtension;

import java.util.function.Consumer;

public class AutoIronDoorBlock extends DoorBlockExtension {
    public AutoIronDoorBlock(Consumer<BlockSettings> consumer) {
        super(false, consumer);
    }
}
