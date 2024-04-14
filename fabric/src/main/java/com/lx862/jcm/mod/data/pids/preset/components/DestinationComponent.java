package com.lx862.jcm.mod.data.pids.preset.components;

import com.lx862.jcm.mod.data.pids.preset.components.base.TextComponent;
import com.lx862.jcm.mod.render.TextOverflowMode;
import com.lx862.jcm.mod.render.text.TextAlignment;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.core.operation.ArrivalsResponse;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

public class DestinationComponent extends TextComponent {
    private final ArrivalsResponse arrivals;
    private final int arrivalIndex;
    public DestinationComponent(ArrivalsResponse arrivals, TextOverflowMode textOverflowMode, TextAlignment textAlignment, int arrivalIndex, String font, int textColor, double x, double y, double width, double height, double scale) {
        super(font, textOverflowMode, textAlignment, textColor, x, y, width, height, scale);
        this.arrivals = arrivals;
        this.arrivalIndex = arrivalIndex;
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, World world, Direction facing) {
        if(arrivalIndex >= arrivals.getArrivals().size()) return;

        ArrivalResponse arrival = arrivals.getArrivals().get(arrivalIndex);
        String routeNo = arrival.getRouteNumber().isEmpty() ? "" : arrival.getRouteNumber() + " ";
        String destinationString = cycleString(routeNo + arrival.getDestination());
        drawText(graphicsHolder, guiDrawing, facing, destinationString);
    }
}
