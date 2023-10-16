package com.lx862.jcm.mod.util;

import com.lx862.jcm.mod.Constants;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.TextHelper;

public class TextUtil {
    public static MutableText literal(String content) {
        return TextHelper.literal(content);
    }

    public static MutableText translatable(TextCategory textCategory, String id, Object... variables) {
        return TextHelper.translatable(textCategory.prefix + "." + Constants.MOD_ID + "." + id, variables);
    }
}