package com.lx862.jcm.mod.data.pids.preset.components;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lx862.jcm.mod.data.JCMClientStats;
import com.lx862.jcm.mod.data.KVPair;
import com.lx862.jcm.mod.data.pids.preset.PIDSContext;
import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;
import com.lx862.jcm.mod.render.text.TextAlignment;
import com.lx862.jcm.mod.render.text.TextOverflowMode;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CycleComponent extends PIDSComponent {
    private final List<PIDSComponent> components;
    private final double cycleTime;

    public CycleComponent(KVPair additionalParam, PIDSComponent... components) {
        super(0, 0, 0, 0);
        this.cycleTime = additionalParam.getInt("cycleTime", 20);
        this.components = new ArrayList<>();
        JsonArray array = additionalParam.get("components", new JsonArray());
        for(int i = 0; i < array.size(); i++) {
            this.components.add(PIDSComponent.parse(array.get(i).getAsJsonObject()));
        }
        for(PIDSComponent component : components) {
            this.components.add(component);
        }
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, Direction facing, PIDSContext context) {
        if(components.isEmpty()) return;
        List<PIDSComponent> filteredComponents = components.stream().filter(e -> e.canRender(context)).collect(Collectors.toList());

        if(cycleTime == -1 && !filteredComponents.isEmpty()) {
            filteredComponents.get(0).render(graphicsHolder, guiDrawing, facing, context); // Render first available component
        } else {
            int currentComponentIndex = (int) ((int)(JCMClientStats.getGameTick() % (cycleTime * filteredComponents.size())) / cycleTime);

            PIDSComponent component = filteredComponents.get(currentComponentIndex);
            component.render(graphicsHolder, guiDrawing, facing, context);
        }
    }

    public static PIDSComponent parseComponent(double x, double y, double width, double height, JsonObject jsonObject) {
        return new CycleComponent(new KVPair(jsonObject));
    }
}