package com.lx862.jcm.mod.gui.widget;

import com.lx862.jcm.mod.render.RenderHelper;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.Widget;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.GuiDrawing;

public class ListEntry {
    public final MutableText title;
    public final Widget widget;
    public final boolean isCategory;
    public DrawIconCallback drawIconCallback = null;

    public ListEntry(MutableText title, Widget widget, boolean isCategory) {
        this.title = title;
        this.widget = widget;
        this.isCategory = isCategory;
    }

    public ListEntry(MutableText title, Widget widget) {
        this.title = title;
        this.widget = widget;
        this.isCategory = false;
    }

    public ListEntry setIcon(Identifier textureId) {
        this.drawIconCallback = new DrawIconCallback() {
            @Override
            public void accept(GuiDrawing guiDrawing, int startX, int startY, int width, int height) {
                drawTexture(guiDrawing, textureId, startX, startY, width, height);
            }
        };
        return this;
    }

    public ListEntry setIconCallback(DrawIconCallback drawIconCallback) {
        this.drawIconCallback = drawIconCallback;
        return this;
    }

    public boolean hasIcon() {
        return this.drawIconCallback != null;
    }

    @FunctionalInterface
    public interface DrawIconCallback extends RenderHelper {
        void accept(GuiDrawing guiDrawing, int startX, int startY, int width, int height);
    }
}
