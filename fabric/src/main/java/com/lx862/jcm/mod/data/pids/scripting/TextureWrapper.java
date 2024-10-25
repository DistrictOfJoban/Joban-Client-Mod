package com.lx862.jcm.mod.data.pids.scripting;

import com.lx862.jcm.mod.render.RenderHelper;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.RenderLayer;
import org.mtr.mapping.mapper.GraphicsHolder;

import static com.lx862.jcm.mod.render.RenderHelper.ARGB_WHITE;
import static com.lx862.jcm.mod.render.RenderHelper.MAX_RENDER_LIGHT;

public class TextureWrapper extends DrawCall {
    public final Identifier id;
    public int color = ARGB_WHITE;

    public TextureWrapper(String id) {
        this(new Identifier(id));
    }

    public TextureWrapper(Identifier id) {
        super(20, 20);
        this.id = id;
    }

    public TextureWrapper pos(double x, double y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public TextureWrapper size(double w, double h) {
        this.w = w;
        this.h = h;
        return this;
    }

    public TextureWrapper color(int color) {
        this.color = color;
        return this;
    }

    @Override
    protected void drawTransformed(GraphicsHolder graphicsHolder, Direction facing) {
        graphicsHolder.createVertexConsumer(RenderLayer.getText(this.id));
        RenderHelper.drawTexture(graphicsHolder, 0, 0, 0, (float)w, (float)h, facing, color, MAX_RENDER_LIGHT);
    }
}
