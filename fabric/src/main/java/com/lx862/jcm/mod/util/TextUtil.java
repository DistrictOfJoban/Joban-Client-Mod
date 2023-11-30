package com.lx862.jcm.mod.util;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.config.ConfigEntry;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.TextHelper;

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

    public static MutableText withFont(MutableText text, Identifier fontId) {
        if(ConfigEntry.USE_CUSTOM_FONT.getBool()) {
            return text.styled(style -> style.withFont(fontId.data));
        }  else {
            return text;
        }
    }
}