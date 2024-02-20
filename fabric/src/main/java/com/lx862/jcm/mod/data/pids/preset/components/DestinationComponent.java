package com.lx862.jcm.mod.data.pids.preset.components;

import com.lx862.jcm.mod.data.pids.preset.components.base.TextComponent;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.render.TextOverflowMode;
import com.lx862.jcm.mod.render.text.TextAlignment;
import com.lx862.jcm.mod.render.text.TextInfo;
import com.lx862.jcm.mod.render.text.TextRenderingManager;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;

public class DestinationComponent extends TextComponent {
    private final ArrivalResponse arrival;
    public DestinationComponent(ArrivalResponse arrival, String font, int textColor, double x, double y, double width, double height, double scale) {
        super(font, textColor, x, y, width, height, scale);
        this.arrival = arrival;
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, World world, Direction facing) {
        String routeNo = arrival.getRouteNumber().isEmpty() ? "" : arrival.getRouteNumber() + " ";
        String destinationString = cycleString(routeNo + arrival.getDestination());
        TextInfo destinationText = new TextInfo(destinationString).withColor(textColor).withFont(font);

        int destinationWidth = TextRenderingManager.getTextWidth(destinationText);

        graphicsHolder.push();
        if(destinationWidth > width) {
            // TODO: Make marquee an option in custom PIDS Preset
            //drawPIDSScrollingText(graphicsHolder, facing, destinationString, 0, 0, textColor, (int)destinationMaxWidth - 2);
        }

        drawText(graphicsHolder, TextAlignment.LEFT, facing, destinationText, TextOverflowMode.STRETCH);
        graphicsHolder.pop();
    }
}
