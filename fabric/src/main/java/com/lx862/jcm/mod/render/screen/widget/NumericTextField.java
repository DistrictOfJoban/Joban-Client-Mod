package com.lx862.jcm.mod.render.screen.widget;

import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.Nullable;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.TextRenderer;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.TextFieldWidgetExtension;
import org.mtr.mapping.tool.TextCase;

/**
 * Text Field Widget that is specifically designed for entering number only
 */
public class NumericTextField extends TextFieldWidgetExtension implements RenderHelper {
    private final long min;
    private final long max;
    private final String prefix;
    private final long defaultValue;

    public NumericTextField(int x, int y, int width, int height, long min, long max, long defaultValue, @Nullable String prefix) {
        super(x, y, width, height, 16, TextCase.LOWER, null, String.valueOf(defaultValue));
        this.min = min;
        this.max = max;
        this.prefix = prefix;
        this.defaultValue = defaultValue;
    }

    public NumericTextField(int x, int y, int width, int height, int min, int max, int defaultValue, MutableText prefix) {
        this(x, y, width, height, min, max, defaultValue, prefix.getString());
    }

    public NumericTextField(int x, int y, int width, int height, int min, int max, int defaultValue) {
        this(x, y, width, height, min, max, defaultValue, (String)null);
    }

    @Override
    public boolean charTyped2(char chr, int modifiers) {
        try {
            int val = Integer.parseInt(getText2() + chr);

            if(val < min || val > max) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }

        return super.charTyped2(chr, modifiers);
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float tickDelta) {
        super.render(graphicsHolder, mouseX, mouseY, tickDelta);
        TextRenderer textRenderer = new TextRenderer(MinecraftClient.getInstance().textRenderer);

        if(prefix != null) {
            drawPrefix(graphicsHolder, textRenderer);
        }

        drawUpDownButton(graphicsHolder, textRenderer);
    }

    private void drawPrefix(GraphicsHolder graphicsHolder, TextRenderer textRenderer) {
        int prefixWidth = GraphicsHolder.getTextWidth(prefix);
        int prefixX = getX2() - prefixWidth;
        int prefixY = getY2() + (getHeight2() / 2) - (textRenderer.getFontHeightMapped() / 2);

        graphicsHolder.drawText(prefix, prefixX, prefixY, 0xFFFFFFFF, true, MAX_RENDER_LIGHT);
    }

    private void drawUpDownButton(GraphicsHolder graphicsHolder, TextRenderer textRenderer) {
        MutableText upArrow = TextUtil.translatable(TextCategory.GUI, "widget.numeric_text_field.increment");
        MutableText dnArrow = TextUtil.translatable(TextCategory.GUI, "widget.numeric_text_field.decrement");
        int fontHeight = textRenderer.getFontHeightMapped();
        int startY = (height - (fontHeight * 2));
        int upWidth = GraphicsHolder.getTextWidth(upArrow);
        int dnWidth = GraphicsHolder.getTextWidth(dnArrow);
        graphicsHolder.drawText(upArrow, getX2() + width - upWidth - 2, getY2() + startY, 0xFFFFFFFF, false, MAX_RENDER_LIGHT);
        graphicsHolder.drawText(dnArrow, getX2() + width - dnWidth - 2, getY2() + startY + fontHeight, 0xFFFFFFFF, false, MAX_RENDER_LIGHT);
    }

    @Override
    public boolean mouseScrolled3(double mouseX, double mouseY, double amount) {
        if(amount > 0) {
            increment();
        } else {
            decrement();
        }
        return super.mouseScrolled3(mouseX, mouseY, amount);
    }

    @Override
    public boolean mouseClicked2(double mouseX, double mouseY, int button) {
        TextRenderer textRenderer = new TextRenderer(MinecraftClient.getInstance().textRenderer);
        MutableText upArrow = TextUtil.translatable(TextCategory.GUI, "widget.numeric_text_field.increment");
        MutableText dnArrow = TextUtil.translatable(TextCategory.GUI, "widget.numeric_text_field.decrement");
        int fontHeight = textRenderer.getFontHeightMapped();
        int startY = getY2() + (height - (fontHeight * 2)) / 2;
        int upWidth = GraphicsHolder.getTextWidth(upArrow.getString());
        int dnWidth = GraphicsHolder.getTextWidth(dnArrow.getString());

        if(inRectangle(mouseX, mouseY, getX2() + width - upWidth - 2, startY, upWidth, fontHeight)) {
            increment();
        }

        if(inRectangle(mouseX, mouseY, getX2() + width - dnWidth - 2, startY + fontHeight, dnWidth, fontHeight)) {
            decrement();
        }

        return super.mouseClicked2(mouseX, mouseY, button);
    }

    public long getValue() {
        try {
            return Integer.parseInt(getText2());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public void setValue(long value) {
        if(value < min || value > max) return;
        setText2(String.valueOf(value));
    }

    private void increment() {
        try {
            setValue(Integer.parseInt(getText2())+1);
        } catch (Exception e) {
            setValue(defaultValue);
        }
    }

    private void decrement() {
        try {
            setValue(Integer.parseInt(getText2())-1);
        } catch (Exception e) {
            setValue(defaultValue);
        }
    }
}
