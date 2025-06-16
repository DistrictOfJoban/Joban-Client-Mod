package com.lx862.jcm.mod.render.gui;

import com.lx862.jcm.mod.JCMClient;
import com.lx862.jcm.mod.data.Pair;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.scripting.jcm.JCMScripting;
import com.lx862.mtrscripting.core.ScriptInstance;
import com.lx862.mtrscripting.data.UniqueKey;
import org.mtr.mapping.mapper.GraphicsHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptDebugOverlay {
    public static void render(GraphicsHolder graphicsHolder) {
        if(!JCMClient.getConfig().debug) return;

        graphicsHolder.translate(10, 10, 0);

        Map<String, List<Pair<UniqueKey, ScriptInstance>>> groupedMap = new HashMap<>();

        for(Map.Entry<UniqueKey, ScriptInstance> map : JCMScripting.getScriptManager().getInstanceManager().getInstances().entrySet()) {
            List<Pair<UniqueKey, ScriptInstance>> existingInstances = groupedMap.getOrDefault(map.getValue().getScript().getDisplayName(), new ArrayList<>());
            if(!map.getValue().shouldInvalidate()) {
                existingInstances.add(new Pair<>(map.getKey(), map.getValue()));
            }
            groupedMap.put(map.getValue().getScript().getDisplayName(), existingInstances);
        }


        for(Map.Entry<String, List<Pair<UniqueKey, ScriptInstance>>> group : groupedMap.entrySet()) {
            graphicsHolder.drawText(group.getKey(), 0, 0, 0xFFCCCCFF, true, RenderHelper.MAX_RENDER_LIGHT);

            graphicsHolder.translate(10, 10, 0);
            for(Pair<UniqueKey, ScriptInstance> scriptInstancePair : group.getValue()) {
                String keyName = scriptInstancePair.getLeft().toString();
                ScriptInstance scriptInstance = scriptInstancePair.getRight();

                double executionMs = scriptInstance.getLastExecutionDurationMs();

                if(scriptInstance.getScript().duringFailCooldown()) {
                    graphicsHolder.drawText(String.format("%s FAILED", keyName), 0, 0, 0xFFFF0000, true, RenderHelper.MAX_RENDER_LIGHT);
                } else {
                    graphicsHolder.drawText(String.format("%s (%.2f ms)", keyName, executionMs), 0, 0, executionMs > (1000/60d) ? 0xFFFFFF00 : 0xFFCCCCFF, true, RenderHelper.MAX_RENDER_LIGHT);
                }

                graphicsHolder.translate(0, 10, 0);
            }
            graphicsHolder.translate(-10, 0, 0);
        }
    }
}
