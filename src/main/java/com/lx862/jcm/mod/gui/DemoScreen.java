package com.lx862.jcm.mod.gui;

import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.Text;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

public class DemoScreen extends BasicScreenBase {
    private static final Identifier TEXTURE_BACKGROUND = new Identifier("jsblock:textures/gui/config_screen/bg.png");
    private static final Identifier TEXTURE_TERRAIN = new Identifier("jsblock:textures/gui/config_screen/terrain.png");

    public DemoScreen() {
        super(true);
    }

    @Override
    public MutableText getScreenTitle() {
        return Text.literal("Demo Screen");
    }

    @Override
    public MutableText getScreenSubtitle() {
        return Text.literal("Hello World ^_^");
    }

    @Override
    protected void init2() {
        super.init2();
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
        super.render(graphicsHolder, mouseX, mouseY, delta);

    }

    @Override
    public void drawBackground(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
        double terrainHeight = (width / 3.75);
        GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
        graphicsHolder.push();
        drawTexture(guiDrawing, TEXTURE_BACKGROUND, 0, 0, width, height);

        double translateY = height * (1 - getEaseAnimation());
        drawTexture(guiDrawing, TEXTURE_TERRAIN, 0, translateY + height - terrainHeight, width, terrainHeight);
        drawPride(graphicsHolder);
        drawSpinningText(graphicsHolder);
        graphicsHolder.pop();
    }

    private void drawSpinningText(GraphicsHolder graphicsHolder) {
        graphicsHolder.push();
        graphicsHolder.translate(width / 2, height / 2, 0);

        int times = 10;
        for(int i = 0; i < times; i++) {
            double radius = 80 * getEaseAnimation();
            double angle = ((i / (double)times) * (Math.PI * 2)) - (elapsed / 200);
            double x = Math.sin(angle) * radius;
            double y = Math.cos(angle) * radius;
            graphicsHolder.push();
            graphicsHolder.translate(x, y, 0);
            graphicsHolder.drawCenteredText("Test", 0, 0, 0xFFFFFFFF);
            graphicsHolder.pop();
        }
        graphicsHolder.pop();
    }

    private void drawPride(GraphicsHolder graphicsHolder) {
        graphicsHolder.push();
        graphicsHolder.rotateYDegrees(5);
        graphicsHolder.scale((float)animationProgress, 1, 1);

        // TODO: where my matrices :(
        GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
        drawRectangle(guiDrawing, (width / 2) - (width / 2 * getEaseAnimation()), 72 + 0, width * getEaseAnimation(), 8, 0xFFDF6277);
        drawRectangle(guiDrawing, (width / 2) - (width / 2 * getEaseAnimation()), 72 + 8, width * getEaseAnimation(), 8, 0xFFFB9168);
        drawRectangle(guiDrawing, (width / 2) - (width / 2 * getEaseAnimation()), 72 + 16, width * getEaseAnimation(), 8, 0xFFF3DB6C);
        drawRectangle(guiDrawing, (width / 2) - (width / 2 * getEaseAnimation()), 72 + 24, width * getEaseAnimation(), 8, 0xFF7AB392);
        drawRectangle(guiDrawing, (width / 2) - (width / 2 * getEaseAnimation()), 72 + 32, width * getEaseAnimation(), 8, 0xFF4B7CBC);
        drawRectangle(guiDrawing, (width / 2) - (width / 2 * getEaseAnimation()), 72 + 40, width * getEaseAnimation(), 8, 0xFF6F488C);
        guiDrawing.finishDrawingRectangle();
        graphicsHolder.pop();
    }
}
