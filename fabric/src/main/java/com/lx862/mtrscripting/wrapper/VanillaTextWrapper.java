package com.lx862.mtrscripting.wrapper;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.TextHelper;

import java.util.Objects;

public class VanillaTextWrapper {
    private final MutableText impl;
    private Style style;
    private boolean skipApplyingStyle;

    public VanillaTextWrapper(MutableText impl) {
        this.impl = impl;
        this.style = Style.getEmptyMapped();
    }

    public static VanillaTextWrapper literal(String str) {
        return new VanillaTextWrapper(TextHelper.literal(str));
    }

    public static VanillaTextWrapper translatable(String str, Object... placeholders) {
        return new VanillaTextWrapper(TextHelper.translatable(str, placeholders));
    }

    public VanillaTextWrapper append(VanillaTextWrapper other) {
        TextHelper.append(this.impl(), other.impl());
        skipApplyingStyle = true;
        return this;
    }

    public VanillaTextWrapper withBold() {
        this.style = this.style.withBold(true);
        return this;
    }

    public VanillaTextWrapper withItalic() {
        this.style = this.style.withItalic(true);
        return this;
    }

    public VanillaTextWrapper withFont(Identifier id) {
        this.style = this.style.withFont(id);
        return this;
    }

    public VanillaTextWrapper withColor(int rgb) {
        this.style = this.style.withColor(TextColor.fromRgb(rgb));
        return this;
    }

    public VanillaTextWrapper withColor(String colorName) {
        TextFormatting textFormatting = TextFormatting.byName(colorName);
        if(textFormatting == null) throw new IllegalArgumentException("Color " + colorName + " is not a valid text color!");
        this.style = this.style.withColor(textFormatting);
        return this;
    }

    public String getString() {
        return this.impl.getString();
    }

    public MutableText impl() {
        return skipApplyingStyle ? this.impl : TextHelper.setStyle(this.impl, this.style);
    }
}
