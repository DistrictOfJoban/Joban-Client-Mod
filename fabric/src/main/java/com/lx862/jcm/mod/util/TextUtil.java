package com.lx862.jcm.mod.util;

import com.lx862.jcm.mod.Constants;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.OrderedText;
import org.mtr.mapping.holder.Style;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.config.Config;
import org.mtr.mod.data.IGui;

import java.util.ArrayList;
import java.util.List;

public class TextUtil {
    public static MutableText literal(String content) {
        return TextHelper.literal(content);
    }

    public static MutableText translatable(TextCategory textCategory, String id, Object... variables) {
        return TextHelper.translatable(textCategory.prefix + "." + Constants.MOD_ID + "." + id, variables);
    }

    public static MutableText translatable(String id, Object... variables) {
        return TextHelper.translatable(id, variables);
    }

    public static Style withFontStyle(Identifier fontId) {
        return Config.getClient().getUseMTRFont() ? Style.getEmptyMapped().withFont(fontId) : Style.getEmptyMapped();
    }

    /** Set a custom font style to MutableText, this respects the client config. */
    public static MutableText withFont(MutableText text, Identifier fontId) {
        return TextHelper.setStyle(text, withFontStyle(fontId));
    }

    public static boolean haveNonCjk(String str) {
        for(String dest : str.split("\\|")) {
            if(!IGui.isCjk(dest)) {
                return true;
            }
        }
        return false;
    }

    public static List<OrderedText> wrapText(MutableText text, int width) {
        List<OrderedText> lines = new ArrayList<>();
        String content = text.getString();
        for(String manualSplitLine : content.split("\n")) {
            GraphicsHolder.wrapLines(TextUtil.literal(manualSplitLine), width).forEach(line -> {
                lines.add(line);
            });
        }

        return lines;
    }
}