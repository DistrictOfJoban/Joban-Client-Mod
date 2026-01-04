package com.lx862.jcm.mod.scripting.jcm.pids;

import com.lx862.jcm.mod.render.RenderHelper;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.RenderLayer;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;
import org.mtr.mod.render.StoredMatrixTransformations;

import static com.lx862.jcm.mod.render.RenderHelper.*;

public class TextureWrapper extends PIDSDrawCall<TextureWrapper> {
    public Identifier id;
    public int color = ARGB_WHITE;
    public float u1 = 0;
    public float v1 = 0;
    public float u2 = 1;
    public float v2 = 1;

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

    public TextureWrapper uv(float u1, float v1, float u2, float v2) {
        this.u1 = u1;
        this.v1 = v1;
        this.u2 = u2;
        this.v2 = v2;
        return this;
    }

    public TextureWrapper uv(float u2, float v2) {
        this.u1 = 0;
        this.v1 = 0;
        this.u2 = u2;
        this.v2 = v2;
        return this;
    }

    @Override
    public void validate() {
        if(id == null) throw new IllegalArgumentException("texture must be filled");
    }

    @Override
    protected void drawTransformed(StoredMatrixTransformations storedMatrixTransformations, Direction facing) {
        MainRenderer.scheduleRender(this.id, false, QueuedRenderLayer.LIGHT_2, (graphicsHolderNew, offset) -> {
//          graphicsHolderNew.push(); // Applied with storedMatrixTransformations.transform
            storedMatrixTransformations.transform(graphicsHolderNew, offset);
            RenderHelper.drawTexture(graphicsHolderNew, 0, 0, 0, (float)w, (float)h, u1, v1, u2, v2, facing, ARGB_BLACK + color, MAX_RENDER_LIGHT);
            graphicsHolderNew.pop();
        });
    }
}
