package com.lx862.jcm.mod.data.pids.preset.components;

import com.lx862.jcm.mod.data.pids.preset.components.base.TextComponent;
import com.lx862.jcm.mod.render.TextOverflowMode;
import com.lx862.jcm.mod.render.text.TextAlignment;
import com.lx862.jcm.mod.render.text.TextInfo;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;

public class CustomTextComponent extends TextComponent {
    private final TextAlignment textAlignment;
    private final TextOverflowMode textOverflowMode;
    private final String text;
    public CustomTextComponent(String font, int textColor, TextAlignment textAlignment, String text, TextOverflowMode textOverflowMode, double x, double y, double width, double height, double scale) {
        super(font, textColor, x, y, width, height, scale);
        this.textAlignment = textAlignment;
        this.text = text;
        this.textOverflowMode = textOverflowMode;
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, World world, Direction facing) {
        drawText(graphicsHolder, textAlignment, facing, new TextInfo(text), textOverflowMode);
    }
}
