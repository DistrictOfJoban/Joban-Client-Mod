package com.lx862.jcm.mod.scripting.pids;

import com.lx862.jcm.mod.render.RenderHelper;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;
import org.mtr.mod.render.StoredMatrixTransformations;

import static com.lx862.jcm.mod.render.RenderHelper.*;

public class TextureWrapper extends PIDSDrawCall<TextureWrapper> {
    protected Identifier textureId;
    protected int color;
    protected float u1;
    protected float v1;
    protected float u2;
    protected float v2;
    protected QueuedRenderLayer renderType;

    protected TextureWrapper() {
        super(20, 20);
        this.textureId = null;
        this.color = ARGB_WHITE;
        this.u1 = 0;
        this.v1 = 0;
        this.u2 = 1;
        this.v2 = 1;
        this.renderType = QueuedRenderLayer.LIGHT_2;
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
        this.textureId = id;
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

    public TextureWrapper renderType(String renderType) {
        this.renderType = QueuedRenderLayer.valueOf(renderType);
        return this;
    }

    @Override
    public void validate() {
        if(this.textureId == null) throw new IllegalArgumentException("texture must be filled");
    }

    @Override
    protected void drawTransformed(StoredMatrixTransformations storedMatrixTransformations, Direction facing) {
        MainRenderer.scheduleRender(this.textureId, false, this.renderType, (graphicsHolderNew, offset) -> {
//          graphicsHolderNew.push(); // Applied with storedMatrixTransformations.transform
            this.storedMatrixTransformations.transform(graphicsHolderNew, offset);
            RenderHelper.drawTexture(graphicsHolderNew, 0, 0, 0, (float)this.w, (float)this.h, this.u1, this.v1, this.u2, this.v2, facing, ARGB_BLACK + this.color, MAX_RENDER_LIGHT);
            graphicsHolderNew.pop();
        });
    }
}
