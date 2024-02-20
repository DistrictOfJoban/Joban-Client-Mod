package com.lx862.jcm.mod.data.pids.preset.components;

import com.lx862.jcm.mod.data.pids.preset.components.base.TextComponent;
import com.lx862.jcm.mod.render.TextOverflowMode;
import com.lx862.jcm.mod.render.text.TextAlignment;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;

public class PlatformComponent extends TextComponent {
    private final ArrivalResponse arrival;
    public PlatformComponent(ArrivalResponse arrival, String font, int textColor, double x, double y, double width, double height) {
        super(font, textColor, x, y, width, height, 1);
        this.arrival = arrival;
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, World world, Direction facing) {
        graphicsHolder.translate(width / 1.6, 2, 0);
        drawText(graphicsHolder, TextAlignment.CENTER, facing, arrival.getPlatformName(), TextOverflowMode.SCALE);
    }
}
