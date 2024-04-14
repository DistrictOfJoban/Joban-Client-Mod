package com.lx862.jcm.mod.data.pids.preset.components.base;

import com.lx862.jcm.mod.render.RenderHelper;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

public abstract class DrawCall implements RenderHelper {
    protected final double x;
    protected final double y;
    protected final double width;
    protected final double height;

    public DrawCall(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Called to render the component
     * @param graphicsHolder The provided graphicsHolder
     * @param guiDrawing guiDrawing, this may be null if not rendered in a 2D context (e.g. GUI Screen)
     * @param world The corresponding world
     * @param facing The direction where the component should be rendered, null if in a 2D context.
     */
    public abstract void render(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, World world, Direction facing);
}
