package com.lx862.jcm.mod.data.pids.scripting;

import org.mtr.mapping.holder.Identifier;

public class TextWrapper {
    public String str;
    public String fontId;
    public boolean shadow;
    public int alignment;
    public int color;

    public TextWrapper(String str) {
        this.str = str;
        this.alignment = -1;
    }

    public TextWrapper leftAlign() {
        this.alignment = -1;
        return this;
    }

    public TextWrapper centerAlign() {
        this.alignment = 0;
        return this;
    }

    public TextWrapper rightAlign() {
        this.alignment = 1;
        return this;
    }

    public TextWrapper shadowed() {
        this.shadow = true;
        return this;
    }

    public TextWrapper font(String fontId) {
        this.fontId = fontId;
        return this;
    }

    public TextWrapper color(int color) {
        this.color = color;
        return this;
    }
}
