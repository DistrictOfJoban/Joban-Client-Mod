package com.lx862.jcm.mod.gui;

import com.lx862.jcm.mod.gui.widget.NumericTextField;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.widget.Widget;
import org.mtr.mapping.holder.ButtonWidget;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.Text;
import org.mtr.mapping.mapper.*;

import static com.lx862.jcm.mod.render.RenderHelper.MAX_RENDER_LIGHT;

public interface GuiHelper {
    int MAX_CONTENT_WIDTH = 400;
    int BOTTOM_ROW_MARGIN = 10;
    static void drawWidget(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float tickDelta, Widget widget) {
        if(widget instanceof ButtonWidgetExtension) {
            ((ButtonWidgetExtension)widget).render(graphicsHolder, mouseX, mouseY, tickDelta);
        } else if(widget instanceof NumericTextField) {
            ((NumericTextField)widget).render(graphicsHolder, mouseX, mouseY, tickDelta);
        } else if(widget instanceof TextFieldWidgetExtension) {
            ((TextFieldWidgetExtension)widget).render(graphicsHolder, mouseX, mouseY, tickDelta);
        } else if(widget instanceof SliderWidgetExtension) {
            ((SliderWidgetExtension)widget).render(graphicsHolder, mouseX, mouseY, tickDelta);
        } else if(widget instanceof CheckboxWidgetExtension) {
            ((CheckboxWidgetExtension)widget).render(graphicsHolder, mouseX, mouseY, tickDelta);
        }
    }

    default void setReadableText(ButtonWidget widget, boolean bl) {
        widget.setMessage(Text.cast(bl ? TextUtil.translatable(TextCategory.GUI, "widget.button.true") : TextUtil.translatable(TextCategory.GUI, "widget.button.false")));
    }

    default void drawScrollableText(GraphicsHolder graphicsHolder, MutableText text, double elapsed, int x, int y, int maxW, int fullHeight, int color, boolean shadow) {
        int textWidth = GraphicsHolder.getTextWidth(text);
        double d = MinecraftClient.getInstance().getWindow().getScaleFactor();
        int i = MinecraftClient.getInstance().getWindow().getFramebufferHeight();

        if(textWidth > maxW) {
            double slideProgress = -((Math.sin(elapsed) / 2) + 0.5);
            graphicsHolder.translate(slideProgress * (textWidth - maxW), 0, 0);
            RenderSystem.setShaderColor(1F, 0.5F, 0.5F, 0.5F);
            // TODO: Scissor not working :(
            RenderSystem.enableScissor((int) (x * d), 0, (int) (maxW * d), i);
            graphicsHolder.drawText(text, x, y, color, shadow, MAX_RENDER_LIGHT);
            RenderSystem.disableScissor();
        } else {
            graphicsHolder.drawText(text, x, y, color, shadow, MAX_RENDER_LIGHT);
        }
    }
}
