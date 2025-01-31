package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.data.pids.preset.PIDSPresetBase;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;

public class RVPIDSSILRenderer<T extends PIDSBlockEntity> extends PIDSRenderer<T> {
    public RVPIDSSILRenderer(Argument dispatcher) {
        super(dispatcher);
    }

    @Override
    public void renderPIDS(T blockEntity, PIDSPresetBase pidsPreset, GraphicsHolder graphicsHolder, World world, BlockState state, BlockPos pos, Direction facing, ObjectArrayList<ArrivalResponse> arrivals, float tickDelta, boolean[] rowHidden) {
        graphicsHolder.translate(-0.21, -0.155, -0.650);
        graphicsHolder.rotateXDegrees(22.5f);
        graphicsHolder.scale(1/96F, 1/96F, 1/96F);
        pidsPreset.render(blockEntity, graphicsHolder, world, blockEntity.getPos2(), facing, arrivals, rowHidden, tickDelta, 0, 0, 136, 76);
    }
}
