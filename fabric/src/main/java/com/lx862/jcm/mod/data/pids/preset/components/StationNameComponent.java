package com.lx862.jcm.mod.data.pids.preset.components;

import com.google.gson.JsonObject;
import com.lx862.jcm.mod.data.KVPair;
import com.lx862.jcm.mod.data.pids.preset.PIDSContext;
import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;
import com.lx862.jcm.mod.data.pids.preset.components.base.TextComponent;
import org.mtr.core.data.Station;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;
import org.mtr.mod.InitClient;
import org.mtr.mod.data.IGui;

public class StationNameComponent extends TextComponent {
    public StationNameComponent(double x, double y, double width, double height, KVPair additionalParam) {
        super(x, y, width, height, additionalParam);
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, Direction facing, PIDSContext context) {
        if(context.pos == null) {
            drawText(graphicsHolder, guiDrawing, facing, "車站|Station");
        } else {
            final Station station = InitClient.findStation(context.pos);
            if(station == null) {
                drawText(graphicsHolder, guiDrawing, facing, IGui.textOrUntitled(""));
            } else {
                drawText(graphicsHolder, guiDrawing, facing, IGui.textOrUntitled(station.getName()));
            }
        }
    }

    public static PIDSComponent parseComponent(double x, double y, double width, double height, JsonObject jsonObject) {
        return new StationNameComponent(x, y, width, height, new KVPair(jsonObject));
    }
}
