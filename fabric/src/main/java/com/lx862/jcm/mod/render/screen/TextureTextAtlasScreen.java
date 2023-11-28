package com.lx862.jcm.mod.render.screen;

import com.lx862.jcm.mod.render.GuiHelper;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.render.screen.base.BasicScreenBase;
import com.lx862.jcm.mod.trm.TextureTextRenderer;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

public class TextureTextAtlasScreen extends BasicScreenBase implements RenderHelper, GuiHelper {
    public TextureTextAtlasScreen() {
        super(true);
    }

    @Override
    public MutableText getScreenTitle() {
        return TextUtil.literal("New Text Renderer");
    }

    @Override
    public MutableText getScreenSubtitle() {
        return TextUtil.literal("Viewing texture atlas");
    }

    @Override
    protected void init2() {
        super.init2();
        ButtonWidgetExtension btn = new ButtonWidgetExtension(
                (int)(width - (width * 0.6)) / 2,
                height - 30,
                (int)(width * 0.6),
                20,
                TextUtil.translatable("gui.done"),
                (b) -> onClose2()
        );

        addChild(new ClickableWidget(btn));
    }

    @Override
    public void drawBackground(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float tickDelta) {
        super.drawBackground(graphicsHolder, mouseX, mouseY, tickDelta);
        drawTextureAtlas(graphicsHolder, new GuiDrawing(graphicsHolder));
    }

    private void drawTextureAtlas(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing) {
        int maxWidth = (int)(width * 0.75);
        int maxHeight = height - 90;

        if(TextureTextRenderer.initialized()) {
            double imgRatio = (double) TextureTextRenderer.getAtlasWidth() / TextureTextRenderer.getAtlasHeight();
            double finalWidth = maxWidth;
            double finalHeight = maxWidth / imgRatio;

            if(finalHeight > maxHeight) {
                finalWidth = maxHeight * imgRatio;
                finalHeight = maxHeight;
            }

            finalWidth *= animationProgress;
            finalHeight *= animationProgress;

            int startX = (width - (int)finalWidth) / 2;
            int startY = (height - (int)finalHeight) / 2;

            drawTexture(guiDrawing, TextureTextRenderer.getAtlasIdentifier(), startX, startY, (int)finalWidth, (int)finalHeight);
        } else {
            graphicsHolder.push();
            graphicsHolder.translate(width / 2.0, height / 2.0, 0);
            graphicsHolder.scale((float)animationProgress, (float)animationProgress, (float)animationProgress);
            RenderHelper.drawCenteredText(graphicsHolder, TextUtil.translatable(TextCategory.GUI, "atlas_config.not_initialized"), 0, 0, ARGB_WHITE);
            graphicsHolder.pop();
        }
    }

    @Override
    public boolean isPauseScreen2() {
        return false;
    }
}
