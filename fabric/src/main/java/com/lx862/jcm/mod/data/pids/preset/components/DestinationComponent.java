package com.lx862.jcm.mod.data.pids.preset.components;

import com.google.gson.JsonObject;
import com.lx862.jcm.mod.data.KVPair;
import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;
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
    private final int arrivalIndex;
    public DestinationComponent(double x, double y, double width, double height,
                                String font, TextAlignment textAlignment, TextOverflowMode textOverflowMode, int textColor, double scale,
                                KVPair additionalParam) {
        super(x, y, width, height, font, textAlignment, textOverflowMode, textColor, scale);
        this.arrivalIndex = additionalParam.get("arrivalIndex", 0);
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, World world, Direction facing, KVPair context) {
        ArrivalsResponse arrivals = context.get("arrivals", null);
        if(arrivalIndex >= arrivals.getArrivals().size()) return;

        ArrivalResponse arrival = arrivals.getArrivals().get(arrivalIndex);
        String routeNo = arrival.getRouteNumber().isEmpty() ? "" : arrival.getRouteNumber() + " ";
        String destinationString = cycleString(routeNo + arrival.getDestination());
        drawText(graphicsHolder, guiDrawing, facing, destinationString);
    }

    public static PIDSComponent parseComponent(double x, double y, double width, double height, JsonObject jsonObject) {
        TextAlignment textAlignment = TextAlignment.valueOf(jsonObject.get("textAlignment").getAsString());
        TextOverflowMode textOverflowMode = TextOverflowMode.valueOf(jsonObject.get("textOverflowMode").getAsString());
        String font = jsonObject.get("font").getAsString();
        int textColor = jsonObject.get("color").getAsInt();
        double scale = jsonObject.get("scale").getAsDouble();
        return new DestinationComponent(x, y, width, height, font, textAlignment, textOverflowMode, textColor, scale, new KVPair(jsonObject));
    }
}
