package com.lx862.jcm.mod.render;

import com.lx862.jcm.mod.render.screen.widget.MappedWidget;
import com.mojang.blaze3d.systems.RenderSystem;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.GraphicsHolder;

import static com.lx862.jcm.mod.render.RenderHelper.MAX_RENDER_LIGHT;

public interface GuiHelper {
    int MAX_CONTENT_WIDTH = 400;
    int BOTTOM_ROW_MARGIN = 6;
    static void drawWidget(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float tickDelta, MappedWidget widget) {
        widget.render(graphicsHolder, mouseX, mouseY, tickDelta);
    }

    /**
     * Draw text that would shift back and fourth if there's not enough space to display
     * Similar to the scrollable text added in Minecraft 1.19.4
     * @param graphicsHolder Graphics holder
     * @param text The text to display
     * @param elapsed The time elapsed, this would dictate the scrolling animation speed
     * @param startX The start X where your text should be clipped. (Measure from the left edge of your window)
     * @param textX The text X that would be rendered
     * @param textY The text Y that would be rendered
     * @param maxW The maximum width allowed for your text
     * @param color Color of the text
     * @param shadow Whether text should be rendered with shadow
     */
    default void drawScrollableText(GraphicsHolder graphicsHolder, MutableText text, double elapsed, int startX, int textX, int textY, int maxW, int color, boolean shadow) {
        int textWidth = GraphicsHolder.getTextWidth(text);
        double scale = MinecraftClient.getInstance().getWindow().getScaleFactor();
        int windowHeight = MinecraftClient.getInstance().getWindow().getHeight();

        if(textWidth > maxW) {
            double slideProgress = ((Math.sin(elapsed / 2)) / 2) + 0.5;
            graphicsHolder.translate(-slideProgress * (textWidth - maxW), 0, 0);
            double clipStartX = startX * scale;
            double clipEndX = maxW * scale;
            RenderSystem.enableScissor((int)clipStartX, 0, (int)clipEndX, windowHeight);
            graphicsHolder.drawText(text, textX, textY, color, shadow, MAX_RENDER_LIGHT);
            RenderSystem.disableScissor();
        } else {
            graphicsHolder.drawText(text, textX, textY, color, shadow, MAX_RENDER_LIGHT);
        }
    }

    default void enableScissor(int x, int y, int width, int height) {
        double scale = net.minecraft.client.MinecraftClient.getInstance().getWindow().getScaleFactor();
        int windowHeight = net.minecraft.client.MinecraftClient.getInstance().getWindow().getHeight();
        RenderSystem.enableScissor((int)(x * scale), (int)(windowHeight - (y + height) * scale), (int)(width * scale), (int)(height * scale));
    }

    default void disableScissor() {
        RenderSystem.disableScissor();
    }
}
