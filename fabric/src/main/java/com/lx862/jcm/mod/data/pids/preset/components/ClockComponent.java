package com.lx862.jcm.mod.data.pids.preset.components;

import com.lx862.jcm.mod.data.pids.preset.components.base.TextComponent;
import com.lx862.jcm.mod.render.TextOverflowMode;
import com.lx862.jcm.mod.render.text.TextAlignment;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;
import org.mtr.mapping.mapper.WorldHelper;

public class ClockComponent extends TextComponent {
    // TODO: (Maybe next version) Add 12/24h toggle
    public ClockComponent(String font, TextOverflowMode textOverflowMode, TextAlignment textAlignment, int textColor, double x, double y, double width, double height) {
        super(font, textOverflowMode, textAlignment, textColor, x, y, width, height);
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, World world, Direction facing) {
        long timeNow = WorldHelper.getTimeOfDay(world) + 6000;
        long hours = timeNow / 1000;
        long minutes = Math.round((timeNow - (hours * 1000)) / 16.8);
        String timeString = String.format("%02d:%02d", hours % 24, minutes % 60);
        drawText(graphicsHolder, guiDrawing, facing, timeString);
    }
}
