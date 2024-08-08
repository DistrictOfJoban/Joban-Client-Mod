package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.block.entity.PIDSProjectorBlockEntity;
import com.lx862.jcm.mod.data.pids.preset.PIDSPresetBase;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;

public class PIDSProjectorRenderer extends PIDSRenderer<PIDSProjectorBlockEntity> {
    public PIDSProjectorRenderer(Argument dispatcher) {
        super(dispatcher);
    }

    @Override
    public void renderPIDS(PIDSProjectorBlockEntity blockEntity, PIDSPresetBase pidsPreset, GraphicsHolder graphicsHolder, World world, BlockState state, BlockPos pos, Direction facing, ObjectArrayList<ArrivalResponse> arrivals, float tickDelta, boolean[] rowHidden) {
        graphicsHolder.translate(-0.21 + blockEntity.getX(), -0.14 + blockEntity.getY(), -0.128 + blockEntity.getZ());
        graphicsHolder.scale(1/96F, 1/96F, 1/96F);
        graphicsHolder.scale(blockEntity.getScale(), blockEntity.getScale(), blockEntity.getScale());
        pidsPreset.render(blockEntity, graphicsHolder, world, blockEntity.getPos2(), facing, arrivals, rowHidden, tickDelta, 0, 0, 136, 76);
    }
}