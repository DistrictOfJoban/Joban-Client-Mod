package com.lx862.jcm.mod.data.pids.preset;

import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.render.text.TextAlignment;
import com.lx862.jcm.mod.render.text.TextInfo;
import com.lx862.jcm.mod.render.text.TextRenderingManager;
import org.jetbrains.annotations.NotNull;
import org.mtr.core.operation.ArrivalsResponse;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;

import javax.annotation.Nullable;

public abstract class PIDSPresetBase implements RenderHelper {
    private final String id;
    private final String name;
    public final boolean builtin;
    public PIDSPresetBase(String id, @Nullable String name, boolean builtin) {
        this.id = id;
        if(name == null) {
            this.name = id;
        } else {
            this.name = name;
        }
        this.builtin = builtin;
    }
    public PIDSPresetBase(String id) {
        this(id, null, false);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    protected void drawPIDSText(GraphicsHolder graphicsHolder, TextAlignment textAlignment, Direction facing, String text, int x, int y, int textColor) {
        drawPIDSText(graphicsHolder, textAlignment, facing, new TextInfo(text), x, y, textColor);
    }

    protected void drawPIDSText(GraphicsHolder graphicsHolder, TextAlignment textAlignment, Direction facing, TextInfo text, int x, int y, int textColor) {
        TextRenderingManager.draw(graphicsHolder, text.withColor(textColor).withFont(getFont()).withTextAlignment(textAlignment), facing, x, y);
    }

    protected void drawPIDSScrollingText(GraphicsHolder graphicsHolder, Direction facing, String text, int x, int y, int textColor, float textWidth) {
        TextRenderingManager.drawScrollingText(graphicsHolder, new TextInfo(text).withColor(textColor), facing, x, y, textWidth);
    }

    public void drawAtlasBackground(GraphicsHolder graphicsHolder, int width, int height, Direction facing) {
        TextRenderingManager.bind(graphicsHolder);
        RenderHelper.drawTexture(graphicsHolder,0, height, 0, width, width, facing, ARGB_WHITE, MAX_RENDER_LIGHT);
    }

    public abstract String getFont();
    public abstract @NotNull Identifier getBackground();
    public abstract int getTextColor();
    public abstract boolean isRowHidden(int row);

    public abstract void render(PIDSBlockEntity be, GraphicsHolder graphicsHolder, World world, Direction facing, ArrivalsResponse arrivals, boolean[] rowHidden, float tickDelta, int x, int y, int width, int height);
}