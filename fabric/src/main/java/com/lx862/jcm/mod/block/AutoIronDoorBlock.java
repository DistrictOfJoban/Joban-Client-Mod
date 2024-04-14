package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.entity.AutoIronDoorBlockEntity;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockSettings;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockWithEntity;
import org.mtr.mapping.mapper.DoorBlockExtension;

import java.util.function.Consumer;

public class AutoIronDoorBlock extends DoorBlockExtension implements BlockWithEntity {
    public AutoIronDoorBlock(Consumer<BlockSettings> consumer) {
        super(false, consumer);
    }

    @Override
    public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new AutoIronDoorBlockEntity(blockPos, blockState);
    }
}
