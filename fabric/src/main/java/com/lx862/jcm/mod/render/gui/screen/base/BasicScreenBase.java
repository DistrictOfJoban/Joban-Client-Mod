package com.lx862.jcm.mod.render.gui.screen.base;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.render.RenderHelper;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.GraphicsHolder;

public abstract class BasicScreenBase extends AnimatableScreenBase {
    public static final int TEXT_PADDING = 10;
    public static final int TITLE_SCALE = 2;
    protected double elapsed = 0;
    public BasicScreenBase(boolean animatable) {
        super(animatable);
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float tickDelta) {
        drawBackground(graphicsHolder, mouseX, mouseY, tickDelta);
        drawTitle(graphicsHolder);
        drawSubtitle(graphicsHolder);

        elapsed += tickDelta / Constants.MC_TICK_PER_SECOND;
        super.render(graphicsHolder, mouseX, mouseY, tickDelta);
    }

    public abstract MutableText getScreenTitle();
    public abstract MutableText getScreenSubtitle();
    public void drawBackground(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float tickDelta) {
        super.renderBackground(graphicsHolder);
    }

    private void drawTitle(GraphicsHolder graphicsHolder) {
        int titleHeight = (textRenderer.fontHeight * TITLE_SCALE);
        MutableText titleText = getScreenTitle();
        graphicsHolder.push();
        graphicsHolder.translate(width / 2.0, TEXT_PADDING, 0);
        graphicsHolder.translate(0, -((titleHeight + TEXT_PADDING) * (1 - animationProgress)), 0);
        graphicsHolder.scale(TITLE_SCALE, TITLE_SCALE, TITLE_SCALE);
        RenderHelper.scaleToFit(graphicsHolder, GraphicsHolder.getTextWidth(titleText), width / TITLE_SCALE, true);
        graphicsHolder.drawCenteredText(titleText, 0, 0, 0xFFFFFFFF);
        graphicsHolder.pop();
    }

    private void drawSubtitle(GraphicsHolder graphicsHolder) {
        double titleHeight = (textRenderer.fontHeight * TITLE_SCALE);
        MutableText subtitleText = getScreenSubtitle();
        graphicsHolder.push();
        graphicsHolder.translate(width / 2.0, titleHeight * animationProgress, 0);
        graphicsHolder.translate(0, TEXT_PADDING * 1.5, 0);
        RenderHelper.scaleToFit(graphicsHolder, GraphicsHolder.getTextWidth(subtitleText), width, true);
        graphicsHolder.drawCenteredText(subtitleText, 0, 0, 0xFFFFFFFF);
        graphicsHolder.pop();
    }
}
