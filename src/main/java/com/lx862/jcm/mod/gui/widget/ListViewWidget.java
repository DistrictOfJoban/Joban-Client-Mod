package com.lx862.jcm.mod.gui.widget;

import com.lx862.jcm.mod.render.Renderable;
import net.minecraft.client.gui.widget.Widget;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.ClickableWidgetExtension;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

import java.util.ArrayList;
import java.util.List;

public class ListViewWidget extends ClickableWidgetExtension implements Renderable {
    private static final int TEXT_PADDING = 5;
    private final int entryHeight;
    private final List<ListEntry> entryList = new ArrayList<>();
    private final List<Double> entryHighlightAnimation = new ArrayList<>();
    public ListViewWidget(int entryHeight) {
        super(0, 0, 0, 0);
        this.entryHeight = entryHeight;
    }

    public void setXYSize(int x, int y, int width, int height) {
        setX2(x);
        setY2(y);
        setWidth2(width);
        setHeight2(height);
        positionWidgets();
    }

    public void add(MutableText text, Widget widget) {
        entryList.add(new ListEntry(text, widget));
        entryHighlightAnimation.add(0.0);
    }

    public void addCategory(MutableText text) {
        entryList.add(new ListEntry(text, null, true));
        entryHighlightAnimation.add(0.0);
    }

    public void reset() {
        this.entryList.clear();
        this.entryHighlightAnimation.clear();
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
        GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
        int x = getX2();
        int y = getY2();
        double animationSpeed = (delta / 4);
        drawRectangle(guiDrawing, x, y, width, height, 0x09FFFFFF);

        for(int i = 0; i < entryList.size(); i++) {
            ListEntry listEntry = entryList.get(i);
            int entryY = y + (i * entryHeight);

            if(!listEntry.isCategory) {
                if(inRectangle(mouseX, mouseY, x, entryY, width, entryHeight)) {
                    entryHighlightAnimation.set(i, Math.min(1, entryHighlightAnimation.get(i) + animationSpeed));
                } else {
                    entryHighlightAnimation.set(i, Math.max(0, entryHighlightAnimation.get(i) - animationSpeed));
                }

                renderEntryHighlight(guiDrawing, i, x, entryY, listEntry.widget.getWidth());
                graphicsHolder.drawText(listEntry.title, x + TEXT_PADDING, entryY + (entryHeight / 4), 0xFFFFFFFF, true, MAX_RENDER_LIGHT);
            } else {
                drawRectangle(new GuiDrawing(graphicsHolder), x, entryY, width, entryHeight, 0xAA999999);
                graphicsHolder.drawCenteredText(listEntry.title, (x + width / 2), entryY + (entryHeight / 4), 0xFFFFFFFF);
            }
        }
    }

    private void positionWidgets() {
        int x = getX2();
        int y = getY2();

        for(int i = 0; i < entryList.size(); i++) {
            ListEntry listEntry = entryList.get(i);
            if(listEntry.isCategory) continue;

            int entryY = y + (i * entryHeight);
            listEntry.widget.setX((x + width) - listEntry.widget.getWidth());
            listEntry.widget.setY(entryY + ((entryHeight - listEntry.widget.getHeight()) / 2));
        }
    }

    private void renderEntryHighlight(GuiDrawing guiDrawing, int entryIndex, int x, int y, int widgetWidth) {
        int highlightAlpha = (int)(80 * entryHighlightAnimation.get(entryIndex));
        int highlightColor = (highlightAlpha << 24) | (190 << 16) | (190 << 8) | 190;

        if(entryHighlightAnimation.get(entryIndex) > 0) {
            drawRectangle(guiDrawing, x, y, width - widgetWidth, entryHeight, highlightColor);
        }
    }
}
