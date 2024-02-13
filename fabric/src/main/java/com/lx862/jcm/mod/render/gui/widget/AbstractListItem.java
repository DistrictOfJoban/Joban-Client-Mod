package com.lx862.jcm.mod.render.gui.widget;

import com.lx862.jcm.mod.render.GuiHelper;
import com.lx862.jcm.mod.render.RenderHelper;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

/**
 * Represent a row in {@link ListViewWidget}
 */
public abstract class AbstractListItem implements RenderHelper, GuiHelper {
    public final int height;
    private double hoverOpacity = 0;

    public AbstractListItem(int height) {
        this.height = height;
    }

    public AbstractListItem() {
        this(22);
    }

    /* */
    public void draw(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, int entryX, int entryY, int width, int height, int mouseX, int mouseY, boolean widgetVisible, double elapsed, float tickDelta) {
        drawBackground(graphicsHolder, guiDrawing, entryX, entryY, width, mouseX, mouseY, widgetVisible, elapsed, tickDelta);
    }

    public abstract boolean matchQuery(String searchTerm);
    public abstract void positionChanged(int entryX, int entryY);
    public abstract void hidden();
    public abstract void shown();

    private void drawBackground(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, int entryX, int entryY, int width, int mouseX, int mouseY, boolean widgetVisible, double elapsed, float tickDelta) {
        double highlightFadeSpeed = (tickDelta / 4);
        boolean entryHovered = inRectangle(mouseX, mouseY, entryX, entryY, width, this.height);
        hoverOpacity = entryHovered ? Math.min(1, hoverOpacity + highlightFadeSpeed) : Math.max(0, hoverOpacity - highlightFadeSpeed);

        if(hoverOpacity > 0) drawListEntryHighlight(guiDrawing, entryX, entryY, width, height);
    }

    private void drawListEntryHighlight(GuiDrawing guiDrawing, int x, int y, int width, int height) {
        int highlightAlpha = (int)(100 * hoverOpacity);
        int highlightColor = (highlightAlpha << 24) | (150 << 16) | (150 << 8) | 150;

        GuiHelper.drawRectangle(guiDrawing, x, y, width, height, highlightColor);
    }
}
