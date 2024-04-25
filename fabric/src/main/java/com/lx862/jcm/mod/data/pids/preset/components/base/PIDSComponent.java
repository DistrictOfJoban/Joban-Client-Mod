package com.lx862.jcm.mod.data.pids.preset.components.base;

import com.google.gson.JsonObject;
import com.lx862.jcm.mod.data.KVPair;
import com.lx862.jcm.mod.data.pids.preset.components.ClockComponent;
import com.lx862.jcm.mod.data.pids.preset.components.CustomTextComponent;
import com.lx862.jcm.mod.data.pids.preset.components.DestinationComponent;
import com.lx862.jcm.mod.data.pids.preset.components.ETAComponent;
import com.lx862.jcm.mod.render.RenderHelper;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

public abstract class PIDSComponent implements RenderHelper {
    protected final double x;
    protected final double y;
    protected final double width;
    protected final double height;

    public PIDSComponent(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Called to render the component
     * @param graphicsHolder The provided graphicsHolder
     * @param guiDrawing guiDrawing, this may be null if not rendered in a 2D context (e.g. GUI Screen)
     * @param world The corresponding world
     * @param facing The direction where the component should be rendered, null if in a 2D context.
     */
    public abstract void render(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, World world, Direction facing, KVPair context);

    public static PIDSComponent parse(JsonObject jsonObject) {
        String name = jsonObject.get("component").getAsString();
        double x = jsonObject.get("x").getAsDouble();
        double y = jsonObject.get("y").getAsDouble();
        double width = jsonObject.get("width").getAsDouble();
        double height = jsonObject.get("height").getAsDouble();

        for(ComponentList component : ComponentList.values()) {
            if(component.name.equals(name)) {
                return component.componentParser.parse(x, y, width, height, jsonObject);
            }
        }
        return null;
    }
}

enum ComponentList {
    CUSTOM_TEXT("customText", CustomTextComponent::parseComponent),
    CLOCK("clock", ClockComponent::parseComponent),
    ARRIVAL_DESTINATION("arrivalDestination", DestinationComponent::parseComponent),
    ARRIVAL_ETA("arrivalETA", ETAComponent::parseComponent);

    public final String name;
    public final ComponentParser componentParser;

    ComponentList(String name, ComponentParser parser) {
        this.name = name;
        this.componentParser = parser;
    }
}

@FunctionalInterface
interface ComponentParser {
    PIDSComponent parse(double x, double y, double width, double height, JsonObject jsonObject);
}