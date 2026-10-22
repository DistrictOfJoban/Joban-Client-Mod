package com.lx862.amberui.gui.widget;

import com.lx862.amberui.gui.GuiHelper;
import com.lx862.jcm.mod.render.RenderHelper;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

import static com.lx862.amberui.gui.widget.ListViewWidget.ENTRY_PADDING;

/**
 * A read-only {@link ListViewWidget} row whose text is re-read from a
 * supplier on every draw — for config rows whose description follows the
 * currently selected value (e.g. "Java 与 C++ 通信方式") without
 * rebuilding the list.
 *
 * <p>The supplier should hand back a pre-built, cached text: menus redraw
 * every frame and the game's zero-allocation discipline applies here too.</p>
 */
public class DynamicTextItem extends AbstractListItem {
    private final java.util.function.Supplier<MutableText> text;
    private final int height;

    public DynamicTextItem(java.util.function.Supplier<MutableText> text, int height) {
        this.text = text;
        this.height = height;
    }

    @Override
    public void draw(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, int entryX, int entryY, int width, int height, int mouseX, int mouseY, boolean widgetVisible, boolean rowInSight, double elapsed, float tickDelta) {
        super.draw(graphicsHolder, guiDrawing, entryX, entryY, width, height, mouseX, mouseY, widgetVisible, rowInSight, elapsed, tickDelta);
        if (!rowInSight) return;
        graphicsHolder.push();
        graphicsHolder.translate(entryX + ENTRY_PADDING, entryY, 0);
        GuiHelper.drawScrollableText(graphicsHolder, text.get(), elapsed,
                entryX + ENTRY_PADDING, 0, (this.height / 2) - 4,
                width - ENTRY_PADDING * 4, ARGB_WHITE, true);
        graphicsHolder.pop();
    }

    @Override
    public boolean matchQuery(String searchTerm) {
        return searchTerm == null || searchTerm.isEmpty();
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
        return height;
    }
}
