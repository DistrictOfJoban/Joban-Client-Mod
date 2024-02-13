package com.lx862.jcm.mod.render.gui.widget;

import com.lx862.jcm.mod.render.GuiHelper;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

import java.util.Objects;

/**
 * Represent a row in {@link ListViewWidget}
 */
public class CategoryItem extends AbstractListItem {
    public final MutableText title;

    public CategoryItem(MutableText title, int height) {
        super(height);
        this.title = title;
    }

    public CategoryItem(MutableText title) {
        super();
        this.title = title;
    }

    /* */
    @Override
    public void draw(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, int entryX, int entryY, int width, int height, int mouseX, int mouseY, boolean widgetVisible, double elapsed, float tickDelta) {
        GuiHelper.drawRectangle(guiDrawing, entryX, entryY, width, this.height, 0x99999999);
        graphicsHolder.drawCenteredText(title, (entryX + width / 2), entryY - (8/2) + (this.height / 2), ARGB_WHITE);
    }

    @Override
    public boolean matchQuery(String searchTerm) {
        return Objects.equals(searchTerm, "") || title.getString().contains(searchTerm);
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
}
