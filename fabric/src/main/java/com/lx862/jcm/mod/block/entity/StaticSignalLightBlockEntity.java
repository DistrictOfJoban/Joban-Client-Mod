package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.registry.BlockEntities;
import org.mtr.mapping.holder.BlockEntityType;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;

public class StaticSignalLightBlockEntity extends JCMBlockEntityBase {
    public StaticSignalLightBlockEntity(SignalType type, BlockPos blockPos, BlockState blockState) {
        super(type.getBlockEntityType(), blockPos, blockState);
    }

    public enum SignalType {
        BLUE(BlockEntities.SIGNAL_LIGHT_BLUE.get()),
        GREEN(BlockEntities.SIGNAL_LIGHT_GREEN.get()),
        RED_BELOW(BlockEntities.SIGNAL_LIGHT_RED_BELOW.get()),
        RED_TOP(BlockEntities.SIGNAL_LIGHT_RED_TOP.get());

        private final BlockEntityType<?> blockEntityType;
        SignalType(BlockEntityType<?> blockEntityType) {
            this.blockEntityType = blockEntityType;
        }

        public BlockEntityType<?> getBlockEntityType() {
            return blockEntityType;
        }
    }
}
