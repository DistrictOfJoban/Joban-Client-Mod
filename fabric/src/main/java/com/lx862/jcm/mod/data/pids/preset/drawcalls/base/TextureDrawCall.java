package com.lx862.jcm.mod.data.pids.preset.drawcalls.base;

import com.lx862.jcm.mod.render.RenderHelper;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.RenderLayer;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;

public class TextureDrawCall extends DrawCall {
    protected final Identifier texture;
    public TextureDrawCall(Identifier texture, double x, double y, double width, double height) {
        super(x, y, width, height);
        this.texture = texture;
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, World world, Direction facing) {
        if(texture != null) {
            drawTexture(graphicsHolder, facing, texture, 0, 0, width, height, ARGB_WHITE);
        }
    }

    public void drawTexture(GraphicsHolder graphicsHolder, Direction facing, Identifier identifier, double x, double y, double width, double height, int color) {
        graphicsHolder.push();
        graphicsHolder.createVertexConsumer(RenderLayer.getText(identifier));
        RenderHelper.drawTexture(graphicsHolder, (float)(this.x + x), (float)(this.y + y), 0, (float)width, (float)height, facing, color, RenderHelper.MAX_RENDER_LIGHT);
        graphicsHolder.pop();
    }
}
