package com.lx862.mtrscripting.mod.gui.widget;

import com.lx862.amberui.gui.GuiHelper;
import org.mtr.mapping.mapper.CheckboxWidgetExtension;
import org.mtr.mapping.mapper.GraphicsHolder;

import java.util.function.Consumer;

public class ConfigCheckbox extends CheckboxWidgetExtension {
    private boolean originalValue = false;

    public ConfigCheckbox(int x, int y, int width, int height, Consumer<Boolean> callback) {
        super(x, y, width, height, false, callback);
    }

    public void setOriginalValue(boolean bl) {
        this.originalValue = bl;
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
        super.render(graphicsHolder, mouseX, mouseY, delta);

        if(this.originalValue != isChecked2()) {
            GuiHelper.drawBorder(graphicsHolder, getX2(), getY2(), getWidth2()-3, getHeight2()-3, 0xFFFFFF88);
        }
    }
}
