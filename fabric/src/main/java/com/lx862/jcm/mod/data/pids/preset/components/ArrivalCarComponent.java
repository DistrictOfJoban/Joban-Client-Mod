package com.lx862.jcm.mod.data.pids.preset.components;

import com.google.gson.JsonObject;
import com.lx862.jcm.mod.data.KVPair;
import com.lx862.jcm.mod.data.pids.preset.PIDSContext;
import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;
import com.lx862.jcm.mod.data.pids.preset.components.base.TextComponent;
import com.lx862.jcm.mod.render.text.TextInfo;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

public class ArrivalCarComponent extends TextComponent {
    private final int arrivalIndex;
    public ArrivalCarComponent(double x, double y, double width, double height, KVPair additionalParam) {
        super(x, y, width, height, additionalParam);
        this.arrivalIndex = additionalParam.getInt("arrivalIndex", 0);
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, Direction facing, PIDSContext context) {
        ObjectArrayList<ArrivalResponse> arrivals = context.arrivals;
        if(arrivalIndex >= arrivals.size()) return;

        ArrivalResponse arrival = arrivals.get(arrivalIndex);
        final MutableText text = TextUtil.literal(arrival.getCarCount() + cycleString("卡", arrival.getCarCount() > 1 ? "-cars" : "-car"));

        drawText(graphicsHolder, guiDrawing, facing, new TextInfo(text));
    }

    public static PIDSComponent parseComponent(double x, double y, double width, double height, JsonObject jsonObject) {
        return new ArrivalCarComponent(x, y, width, height, new KVPair(jsonObject));
    }
}
