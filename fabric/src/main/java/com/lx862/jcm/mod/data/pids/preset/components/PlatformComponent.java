package com.lx862.jcm.mod.data.pids.preset.components;

import com.lx862.jcm.mod.data.pids.preset.components.base.TextComponent;
import com.lx862.jcm.mod.render.TextOverflowMode;
import com.lx862.jcm.mod.render.text.TextAlignment;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.core.operation.ArrivalsResponse;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;

public class PlatformComponent extends TextComponent {
    private final ArrivalsResponse arrivals;
    private final int arrivalIndex;
    public PlatformComponent(ArrivalsResponse arrivals, int arrivalIndex, String font, int textColor, double x, double y, double width, double height) {
        super(font, TextOverflowMode.SCALE, textColor, x, y, width, height, 1);
        this.arrivals = arrivals;
        this.arrivalIndex = arrivalIndex;
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, World world, Direction facing) {
        if(arrivalIndex >= arrivals.getArrivals().size()) return;
        ArrivalResponse arrival = arrivals.getArrivals().get(arrivalIndex);
        graphicsHolder.translate(width / 1.6, 2, 0);
        drawText(graphicsHolder, TextAlignment.CENTER, facing, arrival.getPlatformName());
    }
}
