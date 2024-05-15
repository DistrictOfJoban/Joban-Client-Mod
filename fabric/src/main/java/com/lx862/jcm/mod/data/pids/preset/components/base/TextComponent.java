package com.lx862.jcm.mod.data.pids.preset.components.base;

import com.lx862.jcm.mod.config.ConfigEntry;
import com.lx862.jcm.mod.data.JCMClientStats;
import com.lx862.jcm.mod.data.KVPair;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.render.text.*;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;
import org.mtr.mod.data.IGui;

public abstract class TextComponent extends PIDSComponent {
    public static final int SWITCH_LANG_DURATION = 60;
    protected final TextOverflowMode textOverflowMode;
    protected final TextAlignment textAlignment;
    protected final TextTranslationMode textTranslationMode;
    protected final String font;
    protected final int textColor;
    protected final double scale;

    public TextComponent(double x, double y, double width, double height, KVPair additionalParam) {
        super(x, y, width, height);
        this.font = additionalParam.get("font", "");
        this.textAlignment = TextAlignment.valueOf(additionalParam.get("textAlignment", "LEFT"));
        this.textOverflowMode = TextOverflowMode.valueOf(additionalParam.get("textOverflowMode", "STRETCH"));
        this.textTranslationMode = TextTranslationMode.valueOf(additionalParam.get("textTranslationMode", "CYCLE"));
        this.scale = additionalParam.getDouble("scale", 1.0);
        this.textColor = additionalParam.getColor("color", 0);
    }

    public static KVPair of(TextAlignment textAlignment, TextOverflowMode textOverflowMode, String font, int textColor, double scale) {
        return new KVPair()
                .with("textAlignment", textAlignment.name())
                .with("textOverflowMode", textOverflowMode.name())
                .with("font", font)
                .with("color", textColor)
                .with("scale", scale);
    }

    protected void drawText(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, Direction facing, String text) {
        drawText(graphicsHolder, guiDrawing, facing, new TextInfo(cycleString(text)));
    }

    protected void drawText(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, Direction facing, TextInfo text) {
        TextRenderingManager.bind(graphicsHolder);

        TextInfo finalText = text.withColor(textColor + ARGB_BLACK).withFont(font).withTextAlignment(textAlignment);
        graphicsHolder.push();
        graphicsHolder.translate(x, y, 0);
        graphicsHolder.scale((float)scale, (float)scale, (float)scale);
        double textWidth = TextRenderingManager.getTextWidth(finalText);
        if(textOverflowMode == TextOverflowMode.MARQUEE && textWidth > width) {
            finalText = finalText.withScrollingText();
        } else {
            RenderHelper.scaleToFit(graphicsHolder, textWidth, width, textOverflowMode == TextOverflowMode.SCALE, 14);
        }

        if(guiDrawing != null) {
            if(ConfigEntry.NEW_TEXT_RENDERER.getBool()) {
                TextRenderingManager.draw(graphicsHolder, guiDrawing, finalText, x, y); //HACK: GuiDrawing does not obey graphicsholder.translate
            } else {
                TextRenderingManager.draw(graphicsHolder, guiDrawing, finalText, 0, 0);
            }
        } else {
            TextRenderingManager.draw(graphicsHolder, finalText, facing, 0, 0);
        }

        graphicsHolder.pop();
    }

    protected String cycleString(String mtrString) {
        String[] split = mtrString.split("\\|");
        if(split.length == 0) return "";

        if(textTranslationMode == TextTranslationMode.CYCLE) {
            return split[((int)JCMClientStats.getGameTick() / SWITCH_LANG_DURATION) % split.length];
        }

        if(textTranslationMode == TextTranslationMode.MERGE) {
            return mtrString.replace("|", " ");
        }

        if(textTranslationMode == TextTranslationMode.CJK) {
            for(String str : split) {
                if(IGui.isCjk(str)) {
                    return str;
                }
            }
            return "";
        }

        if(textTranslationMode == TextTranslationMode.NON_CJK) {
            for(String str : split) {
                if(!IGui.isCjk(str)) {
                    return str;
                }
            }
            return "";
        }

        return mtrString;
    }

    protected String cycleString(String... string) {
        return cycleString(String.join("|", string));
    }
}
