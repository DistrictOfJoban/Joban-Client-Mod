package com.lx862.jcm.mod.util;

import com.lx862.jcm.mod.Constants;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.TextHelper;

public class TextUtil {
    public static MutableText getTranslatable(TextCategory textCategory, String id, Object... variables) {
        return TextHelper.translatable(textCategory.prefix + "." + Constants.MOD_ID + "." + id, variables);
    }

    public enum TextCategory {
        HUD("hud"),
        BLOCK("block"),
        ITEM("item"),
        GUI("gui");

        final String prefix;

        TextCategory(String prefix) {
            this.prefix = prefix;
        }
    }
}
