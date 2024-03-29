package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.block.entity.LCDPIDSBlockEntity;
import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.data.pids.preset.PIDSPresetBase;
import org.mtr.core.operation.ArrivalsResponse;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;

public class LCDPIDSRenderer extends PIDSRenderer<LCDPIDSBlockEntity> {
    public LCDPIDSRenderer(Argument dispatcher) {
        super(dispatcher);
    }

    @Override
    public void renderPIDS(PIDSBlockEntity blockEntity, PIDSPresetBase pidsPreset, GraphicsHolder graphicsHolder, World world, BlockState state, BlockPos pos, Direction facing, ArrivalsResponse arrivals, float tickDelta, boolean[] rowHidden) {
        graphicsHolder.scale(0.009F, 0.009F, 0.009F);
        graphicsHolder.rotateYDegrees(90 - facing.asRotation());
        graphicsHolder.rotateZDegrees(180);
        graphicsHolder.translate(-20, -14, -14.25);

        pidsPreset.render(blockEntity, graphicsHolder, world, facing, arrivals, rowHidden, tickDelta, 0, 0, 153, 84);
    }
}
