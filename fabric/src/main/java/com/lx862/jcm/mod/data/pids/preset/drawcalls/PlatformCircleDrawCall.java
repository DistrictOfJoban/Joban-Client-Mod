package com.lx862.jcm.mod.data.pids.preset.drawcalls;

import com.lx862.jcm.mod.data.pids.preset.drawcalls.base.TextureDrawCall;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;

public class PlatformCircleDrawCall extends TextureDrawCall {
    private final ArrivalResponse arrival;
    public PlatformCircleDrawCall(ArrivalResponse arrival, Identifier textureId, double x, double y, double width, double height) {
        super(textureId, x, y, width, height);
        this.arrival = arrival;
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, World world, Direction facing) {
        drawTexture(graphicsHolder, facing, texture, 0, 0, width, height, arrival.getRouteColor() + ARGB_BLACK);
    }
}
