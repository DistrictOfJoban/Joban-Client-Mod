package com.lx862.mtrscripting.core.util.ante;

import com.lx862.mtrscripting.core.integration.VanillaTextWrapper;

/**
 * Backward compatibility with ANTE's ComponentUtil
 */
public class ANTEComponentUtilJS {
    public static VanillaTextWrapper literal(String str) {
        return VanillaTextWrapper.literal(str);
    }

    public static VanillaTextWrapper translatable(String str) {
        return VanillaTextWrapper.translatable(str);
    }

    public static String getString(VanillaTextWrapper textWrapper) {
        return textWrapper.getString();
    }
}
