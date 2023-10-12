package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.block.entity.DepartureTimerBlockEntity;
import org.mtr.mapping.mapper.GraphicsHolder;

public class DepartureTimerRenderer extends JCMBlockEntityRenderer<DepartureTimerBlockEntity> {
    public DepartureTimerRenderer(Argument argument) {
        super(argument);
    }

    @Override
    public void render(DepartureTimerBlockEntity blockEntity, float tickDelta, GraphicsHolder graphicsHolder, int light, int i1) {
        graphicsHolder.push();
        scaleCentered(graphicsHolder, 0.018F, 0.018F, 0.018F);
        rotateToBlockDirection(graphicsHolder, blockEntity);
        graphicsHolder.rotateZDegrees(180);
        graphicsHolder.translate(-12.5, -2, -4.1);
        graphicsHolder.drawText("0:00", 0, 0, 0xFFEE2233, false, MAX_RENDER_LIGHT);
        graphicsHolder.pop();
    }
}
