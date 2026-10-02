package com.lx862.jcm.mod.scripting.pids;

import com.lx862.jcm.mod.render.RenderHelper;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;
import org.mtr.mod.render.StoredMatrixTransformations;

import static com.lx862.jcm.mod.render.RenderHelper.*;

public class RectangleWrapper extends PIDSDrawCall<RectangleWrapper> {
    protected static final Identifier WHITE_TEXTURE = new Identifier("mtr", "textures/block/white.png");
    protected int color;
    protected boolean naturalLight = false;
    protected QueuedRenderLayer renderType;

    protected RectangleWrapper() {
        super(20, 20);
        this.color = ARGB_WHITE;
        this.renderType = QueuedRenderLayer.LIGHT_2;
    }

    public static RectangleWrapper create() {
        return new RectangleWrapper();
    }

    public static RectangleWrapper create(String comment) {
        return create();
    }

    public RectangleWrapper color(int color) {
        this.color = color;
        return this;
    }

    public RectangleWrapper naturalLight() {
        this.naturalLight = true;
        return this;
    }

    public RectangleWrapper renderType(String renderType) {
        this.renderType = QueuedRenderLayer.valueOf(renderType);
        return this;
    }

    @Override
    public void validate() {
    }

    @Override
    protected void drawTransformed(StoredMatrixTransformations storedMatrixTransformations, Direction facing, int light) {
        MainRenderer.scheduleRender(WHITE_TEXTURE, false, this.renderType, (graphicsHolderNew, offset) -> {
//          graphicsHolderNew.push(); // Applied with storedMatrixTransformations.transform
            storedMatrixTransformations.transform(graphicsHolderNew, offset);
            RenderHelper.drawTexture(graphicsHolderNew, 0, 0, 0, (float)this.w, (float)this.h, 0, 0, 1, 1, facing, ARGB_BLACK + this.color, this.naturalLight ? light : MAX_RENDER_LIGHT);
            graphicsHolderNew.pop();
        });
    }
}
