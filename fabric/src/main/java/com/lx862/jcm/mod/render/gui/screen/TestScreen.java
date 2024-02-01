package com.lx862.jcm.mod.render.gui.screen;

import com.lx862.jcm.mod.render.GuiHelper;
import com.lx862.jcm.mod.render.gui.screen.base.BasicScreenBase;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

public class TestScreen extends BasicScreenBase implements GuiHelper {
    private static final Identifier TEXTURE_BACKGROUND = new Identifier("jsblock:textures/gui/config_screen/bg.png");
    private static final Identifier TEXTURE_TERRAIN = new Identifier("jsblock:textures/gui/config_screen/terrain.png");

    public TestScreen() {
        super(true);
    }

    @Override
    public MutableText getScreenTitle() {
        return TextUtil.literal("Demo Screen");
    }

    @Override
    public MutableText getScreenSubtitle() {
        return TextUtil.literal("Hello World :3");
    }

    @Override
    protected void init2() {
        super.init2();
    }

    @Override
    public void drawBackground(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float tickDelta) {
        double terrainHeight = (width / 3.75);
        GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
        GuiHelper.drawTexture(guiDrawing, TEXTURE_BACKGROUND, 0, 0, width, height);

        double translateY = height * (1 - animationProgress);
        GuiHelper.drawTexture(guiDrawing, TEXTURE_TERRAIN, 0, translateY + height - terrainHeight, width, terrainHeight);
        drawPride(graphicsHolder);
        drawSpinningText(graphicsHolder);
    }

    private void drawSpinningText(GraphicsHolder graphicsHolder) {
        graphicsHolder.push();
        graphicsHolder.translate(width / 2.0, height / 2.0, 0);

        int times = 10;
        for(int i = 0; i < times; i++) {
            double radius = 80 * animationProgress;
            double angle = ((i / (double)times) * (Math.PI * 2)) - (elapsed / 10);
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
        graphicsHolder.scale((float) animationProgress, 1, 1);
        double halfWidth = width / 2.0;

        GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
        GuiHelper.drawRectangle(guiDrawing, halfWidth - (halfWidth * animationProgress), 72, width * animationProgress, 8, 0xFFDF6277);
        GuiHelper.drawRectangle(guiDrawing, halfWidth - (halfWidth * animationProgress), 72 + 8, width * animationProgress, 8, 0xFFFB9168);
        GuiHelper.drawRectangle(guiDrawing, halfWidth - (halfWidth * animationProgress), 72 + 16, width * animationProgress, 8, 0xFFF3DB6C);
        GuiHelper.drawRectangle(guiDrawing, halfWidth - (halfWidth * animationProgress), 72 + 24, width * animationProgress, 8, 0xFF7AB392);
        GuiHelper.drawRectangle(guiDrawing, halfWidth - (halfWidth * animationProgress), 72 + 32, width * animationProgress, 8, 0xFF4B7CBC);
        GuiHelper.drawRectangle(guiDrawing, halfWidth - (halfWidth * animationProgress), 72 + 40, width * animationProgress, 8, 0xFF6F488C);
        graphicsHolder.pop();
    }
}
