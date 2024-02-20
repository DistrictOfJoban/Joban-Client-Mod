package com.lx862.jcm.mod.data.pids.preset.components;

import com.lx862.jcm.mod.data.pids.preset.components.base.TextureComponent;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;

public class PlatformCircleComponent extends TextureComponent {
    private final ArrivalResponse arrival;
    public PlatformCircleComponent(ArrivalResponse arrival, Identifier textureId, double x, double y, double width, double height) {
        super(textureId, x, y, width, height);
        this.arrival = arrival;
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, World world, Direction facing) {
        drawTexture(graphicsHolder, facing, texture, 0, 0, width, height, arrival.getRouteColor() + ARGB_BLACK);
    }
}
