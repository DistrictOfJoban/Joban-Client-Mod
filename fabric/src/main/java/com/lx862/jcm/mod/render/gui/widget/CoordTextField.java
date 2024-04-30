package com.lx862.jcm.mod.render.gui.widget;

import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.mapping.mapper.TextFieldWidgetExtension;
import org.mtr.mapping.tool.TextCase;

/**
 * Text Field Widget for entering coordinates (XYZ)
 */
public class CoordTextField extends TextFieldWidgetExtension implements RenderHelper {
    private final long min;
    private final long max;
    private final int defaultValue;

    public CoordTextField(int x, int y, int width, int height, long min, long max, int defaultValue) {
        super(x, y, width, height, 16, TextCase.LOWER, null, String.valueOf(defaultValue));
        this.min = min;
        this.max = max;
        this.defaultValue = defaultValue;
    }

    @Override
    public boolean charTyped2(char chr, int modifiers) {
        String prevValue = getText2();
        boolean bl = super.charTyped2(chr, modifiers);
        try {
            String newString = new StringBuilder(getText2()).insert(getCursor2(), chr).toString().trim();

            // Position
            String[] strSplit = newString.split("\\s+");
            if(strSplit.length > 1) {
                for (String s : strSplit) {
                    if(s.trim().isEmpty()) continue;
                    try {
                        Integer.parseInt(s.trim());
                    } catch (Exception e) {
                        return bl;
                    }
                }
            }

            // Number
            int val = Integer.parseInt(newString);
            if(val < min || val > max) {
                JCMLogger.debug("NumericTextField: Value too large or small");
                setText2(prevValue);
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return bl;
    }

    @Override
    public boolean mouseScrolled2(double mouseX, double mouseY, double amount) {
        if(getVisibleMapped() && getActiveMapped() && isFocused2()) {
            if(amount > 0) {
                increment();
            } else {
                decrement();
            }
        }

        return super.mouseScrolled2(mouseX, mouseY, amount);
    }

    public int getNumber() {
        try {
            return Integer.parseInt(getText2());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public void setValue(long value) {
        if(value < min || value > max) return;
        setText2(String.valueOf(value));
    }

    private void increment() {
        try {
            setValue(Integer.parseInt(getText2())+1);
        } catch (Exception e) {
            setValue(defaultValue);
        }
    }

    private void decrement() {
        try {
            setValue(Integer.parseInt(getText2())-1);
        } catch (Exception e) {
            setValue(defaultValue);
        }
    }
}
