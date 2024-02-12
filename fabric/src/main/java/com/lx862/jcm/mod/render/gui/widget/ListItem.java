package com.lx862.jcm.mod.render.gui.widget;

import com.lx862.jcm.mod.render.GuiHelper;
import com.lx862.jcm.mod.render.RenderHelper;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

import static com.lx862.jcm.mod.render.gui.widget.ListViewWidget.ENTRY_PADDING;

/**
 * Represent a row in {@link ListViewWidget}
 */
public class ListItem implements RenderHelper, GuiHelper {
    public DrawIconCallback drawIconCallback = null;
    public final MutableText title;
    public final MappedWidget widget;
    public final boolean isCategory;
    public final int height;
    private double hoverOpacity = 0;

    public ListItem(MutableText title, MappedWidget widget, boolean isCategory, int height) {
        this.title = title;
        this.widget = widget;
        this.isCategory = isCategory;
        this.height = height;
    }

    public ListItem(MutableText title, MappedWidget widget, boolean isCategory) {
        this(title, widget, isCategory, 22);
    }

    public ListItem(MutableText title, MappedWidget widget) {
        this(title, widget, false);
    }

    public ListItem setIcon(Identifier textureId) {
        this.drawIconCallback = new DrawIconCallback() {
            @Override
            public void accept(GuiDrawing guiDrawing, int startX, int startY, int width, int height) {
                GuiHelper.drawTexture(guiDrawing, textureId, startX, startY, width, height);
            }
        };
        return this;
    }

    public ListItem setIconCallback(DrawIconCallback drawIconCallback) {
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

    /* */
    public void draw(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, int entryX, int entryY, int width, int height, int mouseX, int mouseY, boolean widgetVisible, double elapsed, float tickDelta) {
        if(isCategory) {
            drawListCategory(graphicsHolder, guiDrawing, entryX, entryY, width);
        } else {
            drawListEntry(graphicsHolder, guiDrawing, entryX, entryY, width, mouseX, mouseY, widgetVisible, elapsed, tickDelta);
        }
    }
    private void drawListCategory(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, int entryX, int entryY, int width) {
        GuiHelper.drawRectangle(guiDrawing, entryX, entryY, width, this.height, 0x99999999);
        graphicsHolder.drawCenteredText(title, (entryX + width / 2), entryY - (8/2) + (this.height / 2), ARGB_WHITE);
    }

    private void drawListEntry(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, int entryX, int entryY, int width, int mouseX, int mouseY, boolean widgetVisible, double elapsed, float tickDelta) {
        double highlightFadeSpeed = (tickDelta / 4);
        boolean entryHovered = inRectangle(mouseX, mouseY, entryX, entryY, width, this.height);
        hoverOpacity = entryHovered ? Math.min(1, hoverOpacity + highlightFadeSpeed) : Math.max(0, hoverOpacity - highlightFadeSpeed);

        if(hoverOpacity > 0) drawListEntryHighlight(guiDrawing, entryX, entryY, width, height);

        if(widget != null) {
            widget.setVisible(widgetVisible);

            // We have to draw our widget (Right side) again after rendering the highlight so it doesn't get covered.
            widget.render(graphicsHolder, mouseX, mouseY, tickDelta);
        }

        if(title != null) drawListEntryDescription(graphicsHolder, entryX, entryY, width, elapsed);
    }

    private void drawListEntryDescription(GraphicsHolder graphicsHolder, int entryX, int entryY, int width, double elapsed) {
        int textHeight = 9;
        int iconSize = hasIcon() ? height - ENTRY_PADDING : 0;
        int widgetWidth = widget == null ? 0 : widget.getWidth();
        int availableTextWidth = width - widgetWidth - ENTRY_PADDING - iconSize;
        int textY = (height / 2) - (textHeight / 2);

        graphicsHolder.push();
        graphicsHolder.translate(entryX, entryY, 0);
        graphicsHolder.translate(ENTRY_PADDING, 0, 0);

        if(hasIcon()) {
            drawIconCallback.accept(new GuiDrawing(graphicsHolder), entryX + ENTRY_PADDING, entryY + ((height - iconSize) / 2), iconSize, iconSize);
            graphicsHolder.translate(iconSize + ENTRY_PADDING, 0, 0); // Shift the text to the right
        }

        GuiHelper.drawScrollableText(graphicsHolder, title, elapsed, entryX + ENTRY_PADDING + iconSize, 0, textY, availableTextWidth - iconSize - ENTRY_PADDING - ENTRY_PADDING - ENTRY_PADDING, ARGB_WHITE, true);
        graphicsHolder.pop();
    }
    private void drawListEntryHighlight(GuiDrawing guiDrawing, int x, int y, int width, int height) {
        int highlightAlpha = (int)(100 * hoverOpacity);
        int highlightColor = (highlightAlpha << 24) | (150 << 16) | (150 << 8) | 150;

        GuiHelper.drawRectangle(guiDrawing, x, y, width, height, highlightColor);
    }
}
