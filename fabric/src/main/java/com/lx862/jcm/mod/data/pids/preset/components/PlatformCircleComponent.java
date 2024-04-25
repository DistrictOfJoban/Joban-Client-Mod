package com.lx862.jcm.mod.data.pids.preset.components;

import com.lx862.jcm.mod.data.KVPair;
import com.lx862.jcm.mod.data.pids.preset.components.base.TextureComponent;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.core.operation.ArrivalsResponse;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

public class PlatformCircleComponent extends TextureComponent {
    private final ArrivalsResponse arrivals;
    private final int arrivalIndex;
    public PlatformCircleComponent(ArrivalsResponse arrivals, int arrivalIndex, Identifier textureId, double x, double y, double width, double height) {
        super(textureId, x, y, width, height);
        this.arrivals = arrivals;
        this.arrivalIndex = arrivalIndex;
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, World world, Direction facing, KVPair context) {
        if(arrivalIndex >= arrivals.getArrivals().size()) return;
        ArrivalResponse arrival = arrivals.getArrivals().get(arrivalIndex);
        drawTexture(graphicsHolder, guiDrawing, facing, texture, 0, 0, width, height, arrival.getRouteColor() + ARGB_BLACK);
    }
}
