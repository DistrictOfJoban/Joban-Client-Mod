package com.lx862.jcm.mod.data.pids.preset.components;

import com.google.gson.JsonObject;
import com.lx862.jcm.mod.data.KVPair;
import com.lx862.jcm.mod.data.pids.preset.PIDSContext;
import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;
import com.lx862.jcm.mod.data.pids.preset.components.base.TextComponent;
import com.lx862.jcm.mod.render.TextOverflowMode;
import com.lx862.jcm.mod.render.text.TextAlignment;
import com.lx862.jcm.mod.render.text.TextInfo;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.core.operation.ArrivalsResponse;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ETAComponent extends TextComponent {
    private final boolean useAbsoluteTime;
    private final boolean showDeparture;
    private final int arrivalIndex;
    public ETAComponent(double x, double y, double width, double height, String font, TextAlignment textAlignment, TextOverflowMode textOverflowMode, int textColor, double scale,
                        KVPair context) {
        super(x, y, width, height, font, textAlignment, textOverflowMode, textColor, scale);
        this.arrivalIndex = context.get("arrivalIndex", 0);
        this.showDeparture = context.get("showDeparture", false);
        this.useAbsoluteTime = context.get("useAbsTime", false);
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, Direction facing, PIDSContext context) {
        ArrivalsResponse arrivals = context.arrivals;
        if(arrivalIndex >= arrivals.getArrivals().size()) return;

        ArrivalResponse arrival = arrivals.getArrivals().get(arrivalIndex);
        long arrDepTime = showDeparture ? arrival.getDeparture() : arrival.getArrival();

        long remTime = arrDepTime - System.currentTimeMillis();
        int remSec = (int)(remTime / 1000L);
        if(remSec <= 0) return;

        final MutableText etaText;

        if(useAbsoluteTime) {
            etaText = TextUtil.literal(new SimpleDateFormat("HH:mm").format(new Date(arrival.getArrival())));
        } else {
            if(remSec >= 60) {
                etaText = TextUtil.translatable(cycleString("gui.mtr.arrival_min_cjk", "gui.mtr.arrival_min"), remSec / 60);
            } else {
                etaText = TextUtil.translatable(cycleString("gui.mtr.arrival_sec_cjk", "gui.mtr.arrival_sec"), remSec % 60);
            }
        }

        drawText(graphicsHolder, guiDrawing, facing, new TextInfo(etaText));
    }

    public static PIDSComponent parseComponent(double x, double y, double width, double height, JsonObject jsonObject) {
        TextAlignment textAlignment = TextAlignment.valueOf(jsonObject.get("textAlignment").getAsString());
        TextOverflowMode textOverflowMode = TextOverflowMode.valueOf(jsonObject.get("textOverflowMode").getAsString());
        String font = jsonObject.get("font").getAsString();
        int textColor = jsonObject.get("color").getAsInt();
        double scale = jsonObject.get("scale").getAsDouble();
        return new ETAComponent(x, y, width, height, font, textAlignment, textOverflowMode, textColor, scale, new KVPair(jsonObject));
    }
}
