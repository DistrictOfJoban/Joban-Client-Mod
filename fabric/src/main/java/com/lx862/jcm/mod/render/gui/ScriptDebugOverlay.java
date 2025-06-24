package com.lx862.jcm.mod.render.gui;

import com.lx862.jcm.mod.JCMClient;
import com.lx862.jcm.mod.data.Pair;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.scripting.jcm.JCMScripting;
import com.lx862.jcm.mod.scripting.mtr.MTRScripting;
import com.lx862.jcm.mod.util.TextUtil;
import com.lx862.mtrscripting.core.ScriptInstance;
import com.lx862.mtrscripting.data.UniqueKey;
import com.lx862.mtrscripting.util.GraphicsTexture;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.TextFormatting;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

import java.util.*;

public class ScriptDebugOverlay {
    private static final double IDEAL_FRAMERATE = 60;
    private static final int COLOR_RED = 0xFFFF0000;
    private static final int COLOR_BLUE = 0xFFCCCCFF;
    private static final int COLOR_YELLOW = 0xFFFFFF00;
    private static final int COLOR_WHITE = 0xFFFFFFFF;

    public static void render(GraphicsHolder graphicsHolder) {
        if(!JCMClient.getConfig().debug) return;
        if(MinecraftClient.getInstance().getCurrentScreenMapped() != null) return;
        GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);

        graphicsHolder.translate(10, 10, 0);

        Map<String, List<Pair<UniqueKey, ScriptInstance>>> nameToInstances = getInstancesGroupedByName();

        for(Map.Entry<String, List<Pair<UniqueKey, ScriptInstance>>> group : nameToInstances.entrySet()) {
            MutableText title = TextUtil.literal(group.getKey()).formatted(TextFormatting.UNDERLINE);
            graphicsHolder.drawText(title, 0, 0, COLOR_BLUE, true, RenderHelper.MAX_RENDER_LIGHT);

            graphicsHolder.translate(0, 12, 0);
            graphicsHolder.translate(10, 0, 0);
                int i = 0;
                for(Pair<UniqueKey, ScriptInstance> scriptInstancePair : group.getValue()) {
                    String keyName = scriptInstancePair.getLeft().toString();
                    ScriptInstance scriptInstance = scriptInstancePair.getRight();

                    if(i >= 6) {
                        graphicsHolder.drawText(String.format("... and %d more script instance(s)", group.getValue().size() - i), 0, 0, COLOR_BLUE, true, RenderHelper.MAX_RENDER_LIGHT);
                        graphicsHolder.translate(0, 10, 0);
                        break;
                    }

                    double executionMs = scriptInstance.getLastExecutionDurationMs();

                    if(scriptInstance.getScript().duringFailCooldown()) {
                        graphicsHolder.drawText(String.format("%s FAILED", keyName), 0, 0, COLOR_RED, true, RenderHelper.MAX_RENDER_LIGHT);
                    } else {
                        graphicsHolder.drawText(String.format("%s (%.2f ms)", keyName, executionMs), 0, 0, getColor(executionMs), true, RenderHelper.MAX_RENDER_LIGHT);
                    }

                    graphicsHolder.translate(0, 10, 0);

                    graphicsHolder.translate(10, 0, 0);
                        drawScriptDebugInfo(graphicsHolder, guiDrawing, scriptInstance);
                    graphicsHolder.translate(-10, 0, 0);
                    i++;
                }
            graphicsHolder.translate(-10, 0, 0);
        }
    }

    private static Map<String, List<Pair<UniqueKey, ScriptInstance>>> getInstancesGroupedByName() {
        Map<String, List<Pair<UniqueKey, ScriptInstance>>> groupedMap = new HashMap<>();

        // JCM
        for(Map.Entry<UniqueKey, ScriptInstance> map : JCMScripting.getScriptManager().getInstanceManager().getInstances().entrySet()) {
            List<Pair<UniqueKey, ScriptInstance>> existingInstances = groupedMap.getOrDefault(map.getValue().getScript().getDisplayName(), new ArrayList<>());
            if(!map.getValue().shouldInvalidate()) {
                existingInstances.add(new Pair<>(map.getKey(), map.getValue()));
            }
            groupedMap.put(map.getValue().getScript().getDisplayName(), existingInstances);
        }

        // MTR
        for(Map.Entry<UniqueKey, ScriptInstance> map : MTRScripting.getScriptManager().getInstanceManager().getInstances().entrySet()) {
            List<Pair<UniqueKey, ScriptInstance>> existingInstances = groupedMap.getOrDefault(map.getValue().getScript().getDisplayName(), new ArrayList<>());
            if(!map.getValue().shouldInvalidate()) {
                existingInstances.add(new Pair<>(map.getKey(), map.getValue()));
            }
            groupedMap.put(map.getValue().getScript().getDisplayName(), existingInstances);
        }

        // Sort instance by execution time
        for(List<Pair<UniqueKey, ScriptInstance>> instances : groupedMap.values()) {
            instances.sort((e, f) -> Double.compare(f.getRight().getLastExecutionDurationMs(), e.getRight().getLastExecutionDurationMs()));
        }
        return groupedMap;
    }

    private static void drawScriptDebugInfo(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, ScriptInstance scriptInstance) {
        final int maxTexWidth = MinecraftClient.getInstance().getWindow().getScaledWidth() - 10 - 10 - 10;
        final int maxTexHeight = MinecraftClient.getInstance().getWindow().getScaledHeight() / 3;

        for(Map.Entry<String, Object> debugInfoEntry : scriptInstance.getScriptContext().getDebugInfo()) {
            String key = debugInfoEntry.getKey();
            Object value = debugInfoEntry.getValue();

            graphicsHolder.drawText(String.format("%s: %s", key, value instanceof GraphicsTexture ? "" : value), 0, 0, COLOR_WHITE, true, RenderHelper.MAX_RENDER_LIGHT);
            graphicsHolder.translate(0, 10, 0);

            if(value instanceof GraphicsTexture) {
                GraphicsTexture texture = (GraphicsTexture) value;
                double scale = (double)maxTexWidth / texture.width;
                final int initialHeight = (int)(texture.height * scale);

                if(initialHeight >= maxTexHeight) {
                    scale *= (maxTexHeight / (double)initialHeight);
                }

                final int finalWidth = (int)(texture.width * scale);
                final int finalHeight = (int)(texture.height * scale);

                guiDrawing.beginDrawingTexture(texture.identifier);
                guiDrawing.drawTexture(0, 0, finalWidth, finalHeight, 0, 0, 1, 1);
                guiDrawing.finishDrawingTexture();

                graphicsHolder.translate(0, finalHeight, 0);
            }
        }
    }

    private static int getColor(double executionMs) {
        if(executionMs > (1000/(IDEAL_FRAMERATE/2))) {
            return COLOR_RED;
        } else if(executionMs > (1000/IDEAL_FRAMERATE)) {
            return COLOR_YELLOW;
        } else {
            return COLOR_BLUE;
        }
    }
}
