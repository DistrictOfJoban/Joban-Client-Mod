package com.lx862.jcm.mod.render.gui.widget;

import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.OrderedText;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static com.lx862.jcm.mod.render.gui.widget.ListViewWidget.ENTRY_PADDING;

/**
 * Represent a text-only row in {@link ListViewWidget}
 */
public class DescriptionItem extends AbstractListItem {
    private static final int LINE_HEIGHT = 13;
    private final MutableText text;
    public final List<OrderedText> lines;

    public DescriptionItem(MutableText text, int width) {
        this.text = text;
        this.lines = TextUtil.wrapText(this.text, width - ENTRY_PADDING - ENTRY_PADDING);
    }

    /* */
    @Override
    public void draw(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, int entryX, int entryY, int width, int height, int mouseX, int mouseY, boolean widgetVisible, boolean rowInSight, double elapsed, float tickDelta) {
        super.draw(graphicsHolder, guiDrawing, entryX, entryY, width, height, mouseX, mouseY, widgetVisible, rowInSight, elapsed, tickDelta);
        drawListEntry(graphicsHolder, guiDrawing, entryX, entryY, width, mouseX, mouseY, widgetVisible, rowInSight, elapsed, tickDelta);
    }

    @Override
    protected void drawBackground(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, int entryX, int entryY, int width, int mouseX, int mouseY, boolean widgetVisible, double elapsed, float tickDelta) {
    }

    @Override
    public boolean matchQuery(String searchTerm) {
        return Objects.equals(searchTerm, "") || (text != null && text.getString().toLowerCase(Locale.ROOT).contains(searchTerm.toLowerCase(Locale.ROOT)));
    }

    @Override
    public void positionChanged(int entryX, int entryY) {
    }

    @Override
    public void hidden() {
    }

    @Override
    public void shown() {
    }

    @Override
    public int getHeight() {
        return 10 + (this.lines.size() * LINE_HEIGHT);
    }

    private void drawListEntry(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, int entryX, int entryY, int width, int mouseX, int mouseY, boolean widgetVisible, boolean rowInSight, double elapsed, float tickDelta) {
        if(rowInSight) {
            drawDescription(graphicsHolder, entryX, entryY, width, elapsed);
        }
    }

    private void drawDescription(GraphicsHolder graphicsHolder, int entryX, int entryY, int width, double elapsed) {
        graphicsHolder.push();
        graphicsHolder.translate(entryX, entryY, 0);
        graphicsHolder.translate(ENTRY_PADDING, 0, 0);

        for(int i = 0; i < lines.size(); i++) {
            graphicsHolder.drawText(lines.get(i), 0, ENTRY_PADDING + (i*LINE_HEIGHT), ARGB_WHITE, true, MAX_RENDER_LIGHT);
        }
        graphicsHolder.pop();
    }
}
