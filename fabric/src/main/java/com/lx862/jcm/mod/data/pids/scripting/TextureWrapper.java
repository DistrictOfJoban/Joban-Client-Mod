package com.lx862.jcm.mod.data.pids.scripting;

import com.lx862.jcm.mod.render.RenderHelper;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.RenderLayer;
import org.mtr.mapping.mapper.GraphicsHolder;

import static com.lx862.jcm.mod.render.RenderHelper.*;

public class TextureWrapper extends DrawCall {
    public Identifier id;
    public int color = ARGB_WHITE;

    public TextureWrapper() {
        super(20, 20);
    }

    public static TextureWrapper create() {
        return new TextureWrapper();
    }

    public static TextureWrapper create(String comment) {
        return create();
    }

    public TextureWrapper texture(String id) {
        return texture(new Identifier(id));
    }

    public TextureWrapper texture(Identifier id) {
        this.id = id;
        return this;
    }

    public TextureWrapper color(int color) {
        this.color = color;
        return this;
    }

    @Override
    protected void validate() {
        if(id == null) throw new IllegalArgumentException("texture must be filled");
    }

    @Override
    protected void drawTransformed(GraphicsHolder graphicsHolder, Direction facing) {
        graphicsHolder.createVertexConsumer(RenderLayer.getText(this.id));
        RenderHelper.drawTexture(graphicsHolder, 0, 0, 0, (float)w, (float)h, facing, ARGB_BLACK + color, MAX_RENDER_LIGHT);
    }
}
