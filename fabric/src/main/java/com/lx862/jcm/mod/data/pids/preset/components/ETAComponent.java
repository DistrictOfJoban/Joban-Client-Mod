package com.lx862.jcm.mod.data.pids.preset.components;

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

public class ETAComponent extends TextComponent {
    // TODO: (Maybe next version) Add absolute time?
    private final ArrivalsResponse arrivals;
    private final int arrivalIndex;
    public ETAComponent(ArrivalsResponse arrivals, int arrivalIndex, String font, int textColor, double x, double y, double width, double height, double scale) {
        super(font, TextOverflowMode.STRETCH, textColor, x, y, width, height, scale);
        this.arrivals = arrivals;
        this.arrivalIndex = arrivalIndex;
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, World world, Direction facing) {
        if(arrivalIndex >= arrivals.getArrivals().size()) return;
        ArrivalResponse arrival = arrivals.getArrivals().get(arrivalIndex);
        long remTime = arrival.getArrival() - System.currentTimeMillis();
        int remSec = (int)(remTime / 1000L);
        if(remSec <= 0) return;

        final MutableText etaText;

        if(remSec >= 60) {
            etaText = TextUtil.translatable(cycleString("gui.mtr.arrival_min_cjk", "gui.mtr.arrival_min"), remSec / 60);
        } else {
            etaText = TextUtil.translatable(cycleString("gui.mtr.arrival_sec_cjk", "gui.mtr.arrival_sec"), remSec % 60);
        }

        drawText(graphicsHolder, TextAlignment.RIGHT, facing, new TextInfo(etaText));
    }
}
