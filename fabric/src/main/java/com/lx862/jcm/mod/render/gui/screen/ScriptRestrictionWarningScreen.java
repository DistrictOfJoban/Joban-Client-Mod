package com.lx862.jcm.mod.render.gui.screen;

import com.lx862.jcm.mod.render.GuiHelper;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.render.gui.screen.base.TitledScreen;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.TextFormatting;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.GraphicsHolder;

public class ScriptRestrictionWarningScreen extends TitledScreen implements RenderHelper, GuiHelper {
    private final Runnable acknowledgeWarningCallback;
    public ScriptRestrictionWarningScreen(Runnable acknowledgeWarningCallback) {
        super(false);
        this.acknowledgeWarningCallback = acknowledgeWarningCallback;
    }

    @Override
    public MutableText getScreenTitle() {
        return TextUtil.translatable(TextCategory.GUI, "script_restriction.title").formatted(TextFormatting.YELLOW);
    }

    @Override
    public MutableText getScreenSubtitle() {
        return TextUtil.translatable(TextCategory.GUI, "script_restriction.subtitle");
    }

    @Override
    protected void init2() {
        super.init2();

        ButtonWidgetExtension yesButton = new ButtonWidgetExtension(0, height - 30, 150, 20, TextUtil.translatable("gui.yes"), (b) -> {
                acknowledgeWarningCallback.run();
                onClose2();
            }
        );
        yesButton.setX2((getWidthMapped() / 2) - (TEXT_PADDING/2) - yesButton.getWidth2());
        addChild(new ClickableWidget(yesButton));

        ButtonWidgetExtension noButton = new ButtonWidgetExtension(0, height - 30, 150, 20, TextUtil.translatable("gui.no"), (b) -> onClose2());
        noButton.setX2((getWidthMapped() / 2) + (TEXT_PADDING/2));
        addChild(new ClickableWidget(noButton));
    }

    @Override
    public void drawBackground(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float tickDelta) {
        super.drawBackground(graphicsHolder, mouseX, mouseY, tickDelta);

        int lineHeight = 9;
        int lines = 5;

        graphicsHolder.push();
        graphicsHolder.translate(width / 2.0, height / 2.0, 0);
        graphicsHolder.translate(0, -(lines * lineHeight) / 2.0, 0);
        RenderHelper.drawCenteredText(graphicsHolder, TextUtil.translatable(TextCategory.GUI, "script_restriction.about"), 0, 0, ARGB_WHITE);
        RenderHelper.drawCenteredText(graphicsHolder, TextUtil.translatable(TextCategory.GUI, "script_restriction.consequence").formatted(TextFormatting.UNDERLINE), 0, 10, ARGB_WHITE);
        graphicsHolder.translate(0, lineHeight, 0);
        RenderHelper.drawCenteredText(graphicsHolder, TextUtil.translatable(TextCategory.GUI, "script_restriction.note"), 0, 20, ARGB_WHITE);
        RenderHelper.drawCenteredText(graphicsHolder, TextUtil.translatable(TextCategory.GUI, "script_restriction.caution"), 0, 30, ARGB_WHITE);
        graphicsHolder.pop();
    }

    @Override
    public boolean isPauseScreen2() {
        return false;
    }
}
