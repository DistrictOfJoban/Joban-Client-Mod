package com.lx862.mtrscripting.mod.gui.screen.widget;

import com.lx862.jcm.mod.render.gui.widget.IntegerTextField;
import com.lx862.jcm.mod.util.TextUtil;
import com.lx862.mtrscripting.mod.impl.mtr.eyecandy.config.EyecandyCustomConfig;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class ConfigIntegerField extends IntegerTextField implements ValidatableWidget {
    private final EyecandyCustomConfig.Entry.ValidationCallback validationCallback;
    private Consumer<String> changedListener = null;
    private EyecandyCustomConfig.Entry.ValidationResult result = new EyecandyCustomConfig.Entry.ValidationResult(null);
    private int originalValue = 0;

    public ConfigIntegerField(int x, int y, int width, int height, EyecandyCustomConfig.Entry.ValidationCallback validationCallback) {
        super(x, y, width, height, Integer.MIN_VALUE, Integer.MAX_VALUE, 0);
        this.validationCallback = validationCallback;
        setChangedListener2((newStr) -> {
            if(changedListener != null) {
                changedListener.accept(newStr);
            }
            updateValidation(newStr);
        });
        updateValidation("");
    }

    public void setOriginalValue(int i) {
        this.originalValue = i;
    }

    public void onChange(Consumer<String> callback) {
        this.changedListener = callback;
    }

    @Override
    public void setValue(long i) {
        super.setValue(i);
        updateValidation(String.valueOf(i));
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
        super.render(graphicsHolder, mouseX, mouseY, delta);
        GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);

        if(!this.result.success() && isHovered()) {
            #if MC_VERSION >= "11903"
                MinecraftClient.getInstance().getCurrentScreenMapped().data.setTooltip(List.of(TextUtil.translatable(this.result.validationMessage()).data.asOrderedText()));
            #endif
        } else if(this.originalValue != getNumber()) {
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
