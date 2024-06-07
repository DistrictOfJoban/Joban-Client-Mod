package com.lx862.jcm.mod.render.gui.screen.base;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.TextFormatting;
import org.mtr.mapping.holder.Util;
import org.mtr.mapping.mapper.ClickableWidgetExtension;
import org.mtr.mapping.mapper.GraphicsHolder;

public abstract class TitledScreen extends AnimatedScreen {
    public static final int TEXT_PADDING = 10;
    public static final int TITLE_SCALE = 2;
    protected double elapsed = 0;
    public TitledScreen(boolean animatable) {
        super(animatable);
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float tickDelta) {
        drawBackground(graphicsHolder, mouseX, mouseY, tickDelta);
        drawTitle(graphicsHolder);
        drawSubtitle(graphicsHolder);

        // TODO: Remove this on release
        graphicsHolder.drawText(TextUtil.literal("JCM Beta Release").formatted(TextFormatting.YELLOW), width - GraphicsHolder.getTextWidth("JCM Beta release") - 6, 6, 0xFFFFFFFF, true, GraphicsHolder.getDefaultLight());
        graphicsHolder.drawText(TextUtil.literal("Report issues here!").formatted(TextFormatting.UNDERLINE), width - GraphicsHolder.getTextWidth("Report issues here!") - 6, 18, 0xFFFFFFFF, true, GraphicsHolder.getDefaultLight());
        elapsed += tickDelta / Constants.MC_TICK_PER_SECOND;
        super.render(graphicsHolder, mouseX, mouseY, tickDelta);
    }

    @Override
    public boolean mouseClicked2(double mouseX, double mouseY, int button) {
        int x1 = width - GraphicsHolder.getTextWidth("Report issues here!") - 6;
        int x2 = width - 6;
        int y1 = 18;
        int y2 = 18 + 8;
        if(button == 0 && mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2) {
            Util.getOperatingSystem().open("https://github.com/DistrictOfJoban/Joban-Client-Mod/issues");
            new ClickableWidgetExtension(0, 0, 0, 0).playDownSound2(MinecraftClient.getInstance().getSoundManager());
        }
        return super.mouseClicked2(mouseX, mouseY, button);
    }

    public void drawBackground(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float tickDelta) {
        super.renderBackground(graphicsHolder);
    }

    private void drawTitle(GraphicsHolder graphicsHolder) {
        int titleHeight = (RenderHelper.lineHeight * TITLE_SCALE);
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
        double titleHeight = (RenderHelper.lineHeight * TITLE_SCALE);
        MutableText subtitleText = getScreenSubtitle();
        graphicsHolder.push();
        graphicsHolder.translate(width / 2.0, titleHeight * animationProgress, 0);
        graphicsHolder.translate(0, TEXT_PADDING * 1.5, 0);
        RenderHelper.scaleToFit(graphicsHolder, GraphicsHolder.getTextWidth(subtitleText), width, true);
        graphicsHolder.drawCenteredText(subtitleText, 0, 0, 0xFFFFFFFF);
        graphicsHolder.pop();
    }

    public abstract MutableText getScreenTitle();
    public abstract MutableText getScreenSubtitle();
}
