package com.lx862.jcm.mod.render;

import com.lx862.jcm.mod.render.screen.widget.MappedWidget;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import org.mtr.mapping.holder.ButtonWidget;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.Text;
import org.mtr.mapping.mapper.*;

import static com.lx862.jcm.mod.render.RenderHelper.MAX_RENDER_LIGHT;

public interface GuiHelper {
    int MAX_CONTENT_WIDTH = 400;
    int BOTTOM_ROW_MARGIN = 6;
    static void drawWidget(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float tickDelta, MappedWidget widget) {
        widget.render(graphicsHolder, mouseX, mouseY, tickDelta);
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
            // TODO: Scissor not working :(
            RenderSystem.enableScissor((int) (x * d), 0, (int) (maxW * d), i);
            graphicsHolder.drawText(text, x, y, color, shadow, MAX_RENDER_LIGHT);
            RenderSystem.disableScissor();
        } else {
            graphicsHolder.drawText(text, x, y, color, shadow, MAX_RENDER_LIGHT);
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
