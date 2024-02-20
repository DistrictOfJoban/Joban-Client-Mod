package com.lx862.jcm.mod.data.pids.preset.components.base;

import com.lx862.jcm.mod.render.RenderHelper;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;

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

    public abstract void render(GraphicsHolder graphicsHolder, World world, Direction facing);
}
