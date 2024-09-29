package com.lx862.jcm.mod.data.pids.preset.components.base;

import com.google.gson.JsonObject;
import com.lx862.jcm.mod.data.pids.preset.PIDSContext;
import com.lx862.jcm.mod.data.pids.preset.components.*;
import com.lx862.jcm.mod.render.RenderHelper;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

import java.util.HashMap;
import java.util.Map;

public abstract class PIDSComponent implements RenderHelper {
    /**
     * The full list of available components in a PIDS
     * Other addon developer can add extra components to componentList on mod init
     */
    public static final HashMap<String, ComponentParser> componentList = new HashMap<>();
    protected final double x;
    protected final double y;
    protected final double width;
    protected final double height;

    static {
        componentList.put("arrival_destination", ArrivalDestinationComponent::parseComponent);
        componentList.put("arrival_eta", ArrivalETAComponent::parseComponent);
        componentList.put("arrival_car", ArrivalCarComponent::parseComponent);
        componentList.put("clock", ClockComponent::parseComponent);
        componentList.put("cycle", CycleComponent::parseComponent);
        componentList.put("custom_text", CustomTextComponent::parseComponent);
        componentList.put("custom_texture", CustomTextureComponent::parseComponent);
        componentList.put("platform_text", PlatformComponent::parseComponent);
        componentList.put("station_name", StationNameComponent::parseComponent);
        componentList.put("weather_text", WeatherTextComponent::parseComponent);
        componentList.put("weather_icon", WeatherIconComponent::parseComponent);
    }

    public PIDSComponent(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean canRender(PIDSContext context) {
        return true;
    }

    /**
     * Called to render the component
     * @param graphicsHolder The provided graphicsHolder
     * @param guiDrawing guiDrawing, this may be null if not rendered in a 2D context (e.g. GUI Screen)
     * @param facing The direction where the component should be rendered, null if in a 2D context.
     */
    public abstract void render(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, Direction facing, PIDSContext context);

    public static PIDSComponent parse(JsonObject jsonObject) {
        String name = jsonObject.get("component").getAsString();
        double x = jsonObject.get("x").getAsDouble();
        double y = jsonObject.get("y").getAsDouble();
        double width = jsonObject.get("width").getAsDouble();
        double height = jsonObject.get("height").getAsDouble();

        for(Map.Entry<String, ComponentParser> entry : componentList.entrySet()) {
            if(entry.getKey().equals(name)) {
                return entry.getValue().parse(x, y, width, height, jsonObject);
            }
        }
        return null;
    }
}
