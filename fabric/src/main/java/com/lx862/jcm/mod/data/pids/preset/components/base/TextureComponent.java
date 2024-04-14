package com.lx862.jcm.mod.data.pids.preset.components.base;

import com.lx862.jcm.mod.render.GuiHelper;
import com.lx862.jcm.mod.render.RenderHelper;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.RenderLayer;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

public class TextureComponent extends DrawCall {
    protected final Identifier texture;
    public TextureComponent(Identifier texture, double x, double y, double width, double height) {
        super(x, y, width, height);
        this.texture = texture;
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, World world, Direction facing) {
        if(texture != null) {
            drawTexture(graphicsHolder, guiDrawing, facing, texture, 0, 0, width, height, ARGB_WHITE);
        }
    }

    public void drawTexture(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, Direction facing, Identifier identifier, double offsetX, double offsetY, double width, double height, int color) {
        double finalX = this.x + offsetX;
        double finalY = this.y + offsetY;
        graphicsHolder.push();
        graphicsHolder.createVertexConsumer(RenderLayer.getText(identifier));
        if(guiDrawing != null) {
            GuiHelper.drawTexture(guiDrawing, identifier, finalX, finalY, width, height);
        } else {
            RenderHelper.drawTexture(graphicsHolder, (float)(finalX), (float)(finalY), 0, (float)width, (float)height, facing, color, RenderHelper.MAX_RENDER_LIGHT);
        }
        graphicsHolder.pop();
    }
}
