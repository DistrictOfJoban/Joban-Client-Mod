package com.lx862.jcm.util;

import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.TextHelper;

public class TextUtil {
    public static MutableText getTranslatable(CATEGORY category, String id, Object... variables) {
        return TextHelper.translatable(category.prefix + "." + id, variables);
    }

    public enum CATEGORY {
        HUD("hud"),
        BLOCK("block"),
        ITEM("item"),
        GUI("gui");

        final String prefix;

        CATEGORY(String prefix) {
            this.prefix = prefix;
        }
    }
}
