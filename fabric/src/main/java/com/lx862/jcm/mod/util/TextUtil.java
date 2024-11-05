package com.lx862.jcm.mod.util;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.JCMClient;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.Style;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.data.IGui;

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
        return JCMClient.getConfig().useCustomFont ? Style.getEmptyMapped().withFont(fontId) : Style.getEmptyMapped();
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
}