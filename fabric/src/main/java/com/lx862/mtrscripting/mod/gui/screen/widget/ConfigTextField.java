package com.lx862.mtrscripting.mod.gui.screen.widget;

import com.lx862.jcm.mod.render.ClipStack;
import com.lx862.jcm.mod.render.gui.widget.AlwaysRenderedTextField;
import com.lx862.jcm.mod.util.TextUtil;
import com.lx862.mtrscripting.mod.impl.mtr.eyecandy.config.EyecandyCustomConfig;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;
import org.mtr.mapping.mapper.TextFieldWidgetExtension;
import org.mtr.mapping.tool.TextCase;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class ConfigTextField extends AlwaysRenderedTextField implements ValidatableWidget {
    private final EyecandyCustomConfig.Entry.ValidationCallback validationCallback;
    private Consumer<String> changedListener = null;
    private EyecandyCustomConfig.Entry.ValidationResult result = new EyecandyCustomConfig.Entry.ValidationResult(null);
    private String originalValue = null;

    public ConfigTextField(int x, int y, int width, int height, int maxLength, TextCase textCase, String filter, String suggestion, EyecandyCustomConfig.Entry.ValidationCallback validationCallback) {
        super(x, y, width, height, maxLength, textCase, filter, suggestion);
        this.validationCallback = validationCallback;
        setChangedListener2((newStr) -> {
            if(changedListener != null) {
                changedListener.accept(newStr);
            }
            updateValidation(newStr);
        });
        updateValidation("");
    }

    public void setOriginalValue(String str) {
        this.originalValue = str;
    }

    public void onChange(Consumer<String> callback) {
        this.changedListener = callback;
    }

    @Override
    public void setText2(String str) {
        super.setText2(str);
        updateValidation(str);
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
        ClipStack.ensureStateCorrect();
        super.render(graphicsHolder, mouseX, mouseY, delta);
        GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);

        String content = getText2();
        if(!this.result.success() && isHovered()) {
            #if MC_VERSION >= "11903"
                MinecraftClient.getInstance().getCurrentScreenMapped().data.setTooltip(List.of(TextUtil.translatable(this.result.validationMessage()).data.asOrderedText()));
            #endif
        } else if(!Objects.equals(this.originalValue, content)) {
            guiDrawing.beginDrawingRectangle();
            guiDrawing.drawRectangle(getX2(), getY2(), getX2() + getWidth2(), getY2()+1, 0xFFFFFF88);
            guiDrawing.drawRectangle(getX2(), getY2(), getX2()+1, getY2() + getHeight2(), 0xFFFFFF88);

            guiDrawing.drawRectangle(getX2(), getY2()+getHeight2()-1, getX2() + getWidth2(), getY2()+getHeight2(), 0xFFFFFF88);
            guiDrawing.drawRectangle(getX2()+getWidth2()-1, getY2(), getX2()+getWidth2(), getY2() + getHeight2(), 0xFFFFFF88);
            guiDrawing.finishDrawingRectangle();
        }
    }

    @Override
    public boolean validated() {
        return this.result.success();
    }

    private void updateValidation(String str) {
        EyecandyCustomConfig.Entry.ValidationResult result = validationCallback == null ? new EyecandyCustomConfig.Entry.ValidationResult(null) : validationCallback.validate(str);
        this.result = result;
        setEditableColor2(this.result.success() ? 0xFFFFFFFF : (0xFF000000 | 0xFF4444));
    }
}
