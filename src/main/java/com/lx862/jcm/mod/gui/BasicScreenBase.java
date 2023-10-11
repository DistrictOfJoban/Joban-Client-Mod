package com.lx862.jcm.mod.gui;

import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

public abstract class BasicScreenBase extends AnimatableScreenBase {
    private static final int TEXT_PADDING = 10;
    private static final int TITLE_SCALE = 2;
    protected double elapsed = 0;
    public BasicScreenBase(boolean animatable) {
        super(animatable);
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
        super.render(graphicsHolder, mouseX, mouseY, delta);
        drawBackground(graphicsHolder, mouseX, mouseY, delta);
        drawTitle(graphicsHolder);
        drawSubtitle(graphicsHolder);
        elapsed += delta;
    }

    public abstract MutableText getScreenTitle();
    public abstract MutableText getScreenSubtitle();
    public void drawBackground(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
        super.renderBackground(graphicsHolder);
    }

    private void drawTitle(GraphicsHolder graphicsHolder) {
        graphicsHolder.push();
        graphicsHolder.translate(width / 2.0, TEXT_PADDING, 0);
        graphicsHolder.translate(0, -(TITLE_SCALE * textRenderer.fontHeight + TEXT_PADDING) * (1 - getEaseAnimation()), 0);
        graphicsHolder.scale(TITLE_SCALE, TITLE_SCALE, TITLE_SCALE);
        graphicsHolder.drawCenteredText(getScreenTitle(), 0, 0, 0xFFFFFFFF);
        graphicsHolder.pop();
    }

    private void drawSubtitle(GraphicsHolder graphicsHolder) {
        double titleHeight = (textRenderer.fontHeight * TITLE_SCALE);
        graphicsHolder.push();
        graphicsHolder.translate(width / 2.0, (TEXT_PADDING + (TEXT_PADDING / 2.0)), 0);
        graphicsHolder.translate(0, titleHeight * getEaseAnimation(), 0);
        graphicsHolder.drawCenteredText(getScreenSubtitle(), 0, 0, 0xFFFFFFFF);
        graphicsHolder.pop();
    }

    protected void drawTexture(GuiDrawing guiDrawing, Identifier identifier, double x, double y, double width, double height) {
        guiDrawing.beginDrawingTexture(identifier);
        guiDrawing.drawTexture(x, y, x + width, y + height, 0, 0, 1, 1);
        guiDrawing.finishDrawingTexture();
    }

    protected void drawRectangle(GuiDrawing guiDrawing, double x, double y, double width, double height, int color) {
        guiDrawing.beginDrawingRectangle();
        guiDrawing.drawRectangle(x, y, x + width, y + height, color);
        guiDrawing.finishDrawingRectangle();
    }
}
