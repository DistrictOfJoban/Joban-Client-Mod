package com.lx862.jcm.mod.data.pids.preset.components;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lx862.jcm.mod.data.JCMClientStats;
import com.lx862.jcm.mod.data.KVPair;
import com.lx862.jcm.mod.data.pids.preset.PIDSContext;
import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CycleComponent extends PIDSComponent {
    private List<PIDSComponent> components;
    private double cycleTime;

    public CycleComponent(KVPair additionalParam) {
        super(0, 0, 0, 0);
        this.cycleTime = additionalParam.getDouble("cycleTime", 1);
        this.components = new ArrayList<>();
        JsonArray array = additionalParam.get("components", new JsonArray());
        for(int i = 0; i < array.size(); i++) {
            components.add(PIDSComponent.parse(array.get(i).getAsJsonObject()));
        }
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, Direction facing, PIDSContext context) {
        if(components.isEmpty()) return;
        List<PIDSComponent> filteredComponents = components.stream().filter(e -> e.canRender(context)).collect(Collectors.toList());

        if(cycleTime == -1 && !filteredComponents.isEmpty()) {
            filteredComponents.get(0).render(graphicsHolder, guiDrawing, facing, context); // Render first available component
        } else {
            int currentComponentIndex = (int) ((int)((JCMClientStats.getGameTick() / 20.0) % (cycleTime * filteredComponents.size())) / cycleTime);

            PIDSComponent component = filteredComponents.get(currentComponentIndex);
            component.render(graphicsHolder, guiDrawing, facing, context);
        }
    }

    public static PIDSComponent parseComponent(double x, double y, double width, double height, JsonObject jsonObject) {
        return new CycleComponent(new KVPair(jsonObject));
    }
}