package com.lx862.jcm.mod.data.pids.preset.components;

import com.google.gson.JsonObject;
import com.lx862.jcm.mod.data.KVPair;
import com.lx862.jcm.mod.data.pids.preset.PIDSContext;
import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;
import com.lx862.jcm.mod.data.pids.preset.components.base.TextComponent;
import com.lx862.jcm.mod.render.TextOverflowMode;
import com.lx862.jcm.mod.render.text.TextAlignment;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;
import org.mtr.mapping.mapper.WorldHelper;

public class ClockComponent extends TextComponent {
    private final boolean use24h;
    private final boolean showHour;
    private final boolean showMin;
    private final boolean showAMPM;

    public ClockComponent(double x, double y, double width, double height, KVPair additionalParam) {
        super(x, y, width, height, additionalParam);
        this.use24h = additionalParam.get("use24h", true);
        this.showHour = additionalParam.get("showHour", true);
        this.showMin = additionalParam.get("showMin", true);
        this.showAMPM = additionalParam.get("showAMPM", false);
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, Direction facing, PIDSContext context) {
        long timeNow = WorldHelper.getTimeOfDay(context.world) + 6000;
        long hours = timeNow / 1000;
        long minutes = Math.round((timeNow - (hours * 1000)) / 16.8);
        String str = "";
        if(showHour) {
            if(use24h) str += String.format("%02d", hours % 24);
            else str += hours % 12;
        }
        if(showHour && showMin) str += (":");
        if(showMin) str += String.format("%02d", minutes % 60);
        if(showMin && showAMPM) str += (" ");
        if(showAMPM) str += ((hours % 24 >= 12) ? "PM" : "AM");
        drawText(graphicsHolder, guiDrawing, facing, str);
    }

    public static PIDSComponent parseComponent(double x, double y, double width, double height, JsonObject jsonObject) {
        return new ClockComponent(x, y, width, height, new KVPair(jsonObject));
    }
}
