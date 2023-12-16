package com.lx862.jcm.mod.render.screen.widget;

import com.lx862.jcm.mod.render.GuiHelper;
import com.lx862.jcm.mod.render.RenderHelper;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.GuiDrawing;

/**
 * Represent a row in {@link ListViewWidget}
 */
public class ListEntry implements GuiHelper {
    public final MutableText title;
    public final MappedWidget widget;
    public final boolean isCategory;
    public final int height;
    public DrawIconCallback drawIconCallback = null;

    public ListEntry(MutableText title, MappedWidget widget, boolean isCategory, int height) {
        this.title = title;
        this.widget = widget;
        this.isCategory = isCategory;
        this.height = height;
    }

    public ListEntry(MutableText title, MappedWidget widget, boolean isCategory) {
        this(title, widget, isCategory, 22);
    }

    public ListEntry(MutableText title, MappedWidget widget) {
        this(title, widget, false);
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
