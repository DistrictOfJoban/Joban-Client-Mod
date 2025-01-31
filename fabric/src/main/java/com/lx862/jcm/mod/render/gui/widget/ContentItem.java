package com.lx862.jcm.mod.render.gui.widget;

import com.lx862.jcm.mod.render.GuiHelper;
import com.lx862.jcm.mod.render.RenderHelper;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

import java.util.Objects;

import static com.lx862.jcm.mod.render.gui.widget.ListViewWidget.ENTRY_PADDING;

/**
 * Represent a row in {@link ListViewWidget}
 */
public class ContentItem extends AbstractListItem {
    public DrawIconCallback drawIconCallback = null;
    public final MutableText title;
    public final MappedWidget widget;

    public ContentItem(MutableText title, MappedWidget widget, int height) {
        super(height);
        this.title = title;
        this.widget = widget;
    }

    public ContentItem(MutableText title, MappedWidget widget) {
        this(title, widget, 22);
    }

    public ContentItem setIcon(Identifier textureId) {
        this.drawIconCallback = (guiDrawing, startX, startY, width, height) -> GuiHelper.drawTexture(guiDrawing, textureId, startX, startY, width, height);
        return this;
    }

    public ContentItem setIconCallback(DrawIconCallback drawIconCallback) {
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
        super.draw(graphicsHolder, guiDrawing, entryX, entryY, width, height, mouseX, mouseY, widgetVisible, elapsed, tickDelta);
        drawListEntry(graphicsHolder, guiDrawing, entryX, entryY, width, mouseX, mouseY, widgetVisible, elapsed, tickDelta);
    }

    @Override
    public boolean matchQuery(String searchTerm) {
        return Objects.equals(searchTerm, "") || (title != null && title.getString().contains(searchTerm));
    }

    @Override
    public void positionChanged(int entryX, int entryY) {
        if(widget != null) {
            int offsetY = (height - widget.getHeight()) / 2;
            widget.setX(entryX - widget.getWidth());
            widget.setY(entryY + offsetY);
        }
    }

    @Override
    public void hidden() {
        if(widget != null) {
            widget.setVisible(false);
        }
    }

    @Override
    public void shown() {
        if(widget != null) {
            widget.setVisible(true);
        }
    }

    private void drawListEntry(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, int entryX, int entryY, int width, int mouseX, int mouseY, boolean widgetVisible, double elapsed, float tickDelta) {
        if(title != null) drawListEntryDescription(graphicsHolder, entryX, entryY, width, elapsed);

        if(widget != null) {
            widget.setVisible(widgetVisible);
            // We have to draw our widget (Right side) again after rendering the highlight so it doesn't get covered.
            widget.render(graphicsHolder, mouseX, mouseY, tickDelta);
        }
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
            drawIconCallback.accept(new GuiDrawing(graphicsHolder), 0, ((height - iconSize) / 2), iconSize, iconSize);
            graphicsHolder.translate(iconSize + ENTRY_PADDING, 0, 0); // Shift the text to the right
        }

        GuiHelper.drawScrollableText(graphicsHolder, title, elapsed, entryX + ENTRY_PADDING + iconSize, 0, textY, availableTextWidth - iconSize - ENTRY_PADDING - ENTRY_PADDING - ENTRY_PADDING, ARGB_WHITE, true);
        graphicsHolder.pop();
    }
}
