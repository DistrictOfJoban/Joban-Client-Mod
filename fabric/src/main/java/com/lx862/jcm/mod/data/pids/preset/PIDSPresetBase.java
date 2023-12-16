package com.lx862.jcm.mod.data.pids.preset;

import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.trm.TextInfo;
import com.lx862.jcm.mod.trm.TextRenderingManager;
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

    protected void drawPIDSText(GraphicsHolder graphicsHolder, Direction facing, String text, int x, int y, int textColor) {
        TextRenderingManager.draw(graphicsHolder, new TextInfo(text).withColor(textColor).withFont(getFont()), facing, x, y);
    }

    protected void drawPIDSScrollingText(GraphicsHolder graphicsHolder, Direction facing, String text, int x, int y, int textColor, float textWidth) {
        TextRenderingManager.drawScrollingText(graphicsHolder, new TextInfo(text).withColor(textColor), facing, x, y, textWidth);
    }

    protected void drawPIDSCenteredText(GraphicsHolder graphicsHolder, Direction facing, String text, int x, int y, int textColor) {
        TextRenderingManager.drawCentered(graphicsHolder, new TextInfo(text).withColor(textColor).withFont(getFont()), facing, x, y);
    }

    public abstract String getFont();
    public abstract Identifier getPreviewImage();

    public abstract void render(PIDSBlockEntity be, GraphicsHolder graphicsHolder, World world, Direction facing, float tickDelta, int x, int y, int width, int height, int light, int overlay);

    @FunctionalInterface
    public interface DrawRowCallback extends RenderHelper {
        void accept(GraphicsHolder graphicsHolder, int width);
    }
}