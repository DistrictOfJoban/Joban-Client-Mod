package com.lx862.jcm.mod.render.text;

import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.TextFormatting;

public class TextInfo {
    private final String content;
    private Identifier fontId;
    private TextAlignment textAlignment = TextAlignment.LEFT;
    private int textColor;

    public TextInfo(String content) {
        this.content = content;
    }

    public TextInfo(MutableText text) {
        this.content = text.getString();
    }

    public String getContent() {
        return content;
    }

    public int getTextColor() {
        return textColor;
    }

    public TextAlignment getTextAlignment() {
        return textAlignment;
    }

    public TextInfo withColor(int color) {
        this.textColor = color;
        return this;
    }

    public TextInfo withTextAlignment(TextAlignment textAlignment) {
        this.textAlignment = textAlignment;
        return this;
    }

    public TextInfo withFont(String font) {
        return withFont(new Identifier(font));
    }

    public TextInfo withFont(Identifier fontId) {
        this.fontId = fontId;
        return this;
    }

    public MutableText toMutableText() {
        MutableText text = TextUtil.literal(content);
        if(fontId != null) {
            text = TextUtil.withFont(text, fontId);
        }
        return text;
    }

    public TextInfo copy(String newContent) {
        TextInfo ti = new TextInfo(newContent);
        ti.withColor(textColor);
        ti.withFont(fontId);
        return ti;
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof TextInfo)) return false;
        TextInfo other = ((TextInfo) o);
        return other.content.equals(content) && other.textColor == textColor && this.fontId.equals(other.fontId);
    }
}
