package com.lx862.jcm.mod.render.gui.widget;

import org.jetbrains.annotations.Nullable;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.TextFieldWidgetExtension;
import org.mtr.mapping.tool.TextCase;

public class AlwaysRenderedTextField extends TextFieldWidgetExtension {
    public AlwaysRenderedTextField(int x, int y, int width, int height, int maxLength, TextCase textCase, @Nullable String filter, @Nullable String suggestion) {
        super(x, y, width, height, maxLength, textCase, filter, suggestion);
    }

    public AlwaysRenderedTextField(int x, int y, int width, int height, String message, int maxLength, TextCase textCase, @Nullable String filter, @Nullable String suggestion) {
        super(x, y, width, height, message, maxLength, textCase, filter, suggestion);
    }

    public AlwaysRenderedTextField(int x, int y, int width, int height, MutableText text, int maxLength, TextCase textCase, @Nullable String filter, @Nullable String suggestion) {
        super(x, y, width, height, text, maxLength, textCase, filter, suggestion);
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float tickDelta) {
        boolean wasVisible = isVisible2();
        setVisible2(true);
        super.render(graphicsHolder, mouseX, mouseY, tickDelta);
        setVisible2(wasVisible);
    }
}
