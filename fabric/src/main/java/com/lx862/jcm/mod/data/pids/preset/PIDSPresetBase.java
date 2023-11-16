package com.lx862.jcm.mod.data.pids.preset;

import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.trm.TextRenderingManager;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.TextRenderer;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;

import javax.annotation.Nullable;

public abstract class PIDSPresetBase implements RenderHelper {
    private final String id;
    private final String name;
    public PIDSPresetBase(String id, @Nullable String name) {
        this.id = id;
        if(name == null) {
            this.name = id;
        } else {
            this.name = name;
        }
    }
    public PIDSPresetBase(String id) {
        this(id, null);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    protected void drawPIDSText(GraphicsHolder graphicsHolder, Direction facing, String text, int x, int y, int textColor) {
        TextRenderingManager.draw(graphicsHolder, text, facing, x, y, textColor);
        //drawText(graphicsHolder, TextUtil.withFont(TextUtil.literal(text), getFont()), x, y, textColor);
    }

    protected void drawPIDSCenteredText(GraphicsHolder graphicsHolder, Direction facing, String text, int x, int y, int textColor) {
        TextRenderingManager.drawCentered(graphicsHolder, text, facing, x, y, textColor);
        //drawCenteredText(graphicsHolder, TextUtil.withFont(TextUtil.literal(text), getFont()), x, y, textColor);
    }

    public String getFont() {
        return null;
    }

    public abstract void render(PIDSBlockEntity be, GraphicsHolder graphicsHolder, World world, Direction facing, float tickDelta, int x, int y, int width, int height, int light, int overlay);

    @FunctionalInterface
    public interface DrawRowCallback extends RenderHelper {
        void accept(GraphicsHolder graphicsHolder, int width);
    }
}