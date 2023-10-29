package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.block.entity.DepartureTimerBlockEntity;
import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.data.pids.PIDSManager;
import org.mtr.mapping.mapper.GraphicsHolder;

public class RVPIDSRenderer extends JCMBlockEntityRenderer<PIDSBlockEntity> {
    public RVPIDSRenderer(Argument argument) {
        super(argument);
    }

    @Override
    public void renderCurated(PIDSBlockEntity blockEntity, float tickDelta, GraphicsHolder graphicsHolder, int light, int i1) {
        graphicsHolder.push();
        rotateToBlockDirection(graphicsHolder, blockEntity);
        graphicsHolder.translate(0.36F, -0.35F, 0F);
        graphicsHolder.translate(0, 1, 0.25F);
        graphicsHolder.scale(0.02F, 0.02F, 0.02F);
        graphicsHolder.translate(0, 1, -0.25F);
        graphicsHolder.rotateYDegrees(90);
        graphicsHolder.rotateZDegrees(180);
        PIDSManager.builtInPreset.get("rv_pids").render(blockEntity, graphicsHolder, blockEntity.getWorld2(), tickDelta, 0, 0, 75, 40, 0xFFFFFFFF, MAX_RENDER_LIGHT);
        graphicsHolder.drawText("Testing 123", 0, 0, 0xFFFFFFFF, false, MAX_RENDER_LIGHT);
        graphicsHolder.pop();
    }
}
