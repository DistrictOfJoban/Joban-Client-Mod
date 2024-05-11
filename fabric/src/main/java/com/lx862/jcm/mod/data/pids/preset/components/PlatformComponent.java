package com.lx862.jcm.mod.data.pids.preset.components;

import com.google.gson.JsonObject;
import com.lx862.jcm.mod.data.KVPair;
import com.lx862.jcm.mod.data.pids.preset.PIDSContext;
import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;
import com.lx862.jcm.mod.data.pids.preset.components.base.TextComponent;
import com.lx862.jcm.mod.render.text.TextOverflowMode;
import com.lx862.jcm.mod.render.text.TextAlignment;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.core.tool.Utilities;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

public class PlatformComponent extends TextComponent {
    private final int arrivalIndex;
    public PlatformComponent(double x, double y, double width, double height, String font, int textColor, double scale, KVPair additionalParam) {
        super(x, y, width, height, TextComponent.of(TextAlignment.CENTER, TextOverflowMode.SCALE, font, textColor, scale));
        this.arrivalIndex = additionalParam.get("arrivalIndex", 0);
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, Direction facing, PIDSContext context) {
        ArrivalResponse arrival = Utilities.getElement(context.arrivals, arrivalIndex);
        if(arrival == null) return;

        graphicsHolder.translate(width / 1.6, 2, 0);
        drawText(graphicsHolder, guiDrawing, facing, arrival.getPlatformName());
    }

    public static PIDSComponent parseComponent(double x, double y, double width, double height, JsonObject jsonObject) {
        String font = jsonObject.get("font").getAsString();
        int textColor = jsonObject.get("color").getAsInt();
        double scale = jsonObject.get("scale").getAsDouble();
        return new PlatformComponent(x, y, width, height, font, textColor, scale, new KVPair(jsonObject));
    }
}
