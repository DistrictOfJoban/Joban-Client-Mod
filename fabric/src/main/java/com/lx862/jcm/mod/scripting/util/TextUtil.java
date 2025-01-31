package com.lx862.jcm.mod.scripting.util;

// On behalf of MTR
/* From https://github.com/zbx1425/mtr-nte/blob/master/common/src/main/java/cn/zbx1425/mtrsteamloco/render/scripting/util/TextUtil.java*/

import org.mtr.mod.InitClient;
import org.mtr.mod.data.IGui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.lx862.jcm.mod.data.pids.preset.components.base.TextComponent.SWITCH_LANG_DURATION;

public class TextUtil {

    public static String getCjkParts(String src) {
        return getCjkMatching(src, true);
    }

    public static String getNonCjkParts(String src) {
        return getCjkMatching(src, false);
    }

    public static String getExtraParts(String src) {
        return getExtraMatching(src, true);
    }

    public static String getNonExtraParts(String src) {
        return getExtraMatching(src, false);
    }

    public static String getNonCjkAndExtraParts(String src) {
        String extraParts = getExtraMatching(src, false).trim();
        return getCjkMatching(src, false).trim() + (extraParts.isEmpty() ? "" : "|" + extraParts);
    }

    public static boolean isCjk(String src) {
        return IGui.isCjk(src);
    }

    public static String cycleString(String mtrString) {
        return cycleString(mtrString, SWITCH_LANG_DURATION);
    }

    public static String cycleString(String mtrString, int switchDuration) {
        if(mtrString == null) return "";

        List<String> split = new ArrayList<>(Arrays.asList(mtrString.split("\\|")));
        if(split.isEmpty()) return "";

        return split.get(((int) InitClient.getGameTick() / switchDuration) % split.size());
    }

    private static String getExtraMatching(String src, boolean extra) {
        if (src.contains("||")) {
            return src.split("\\|\\|", 2)[extra ? 1 : 0].trim();
        } else {
            return "";
        }
    }

    private static String getCjkMatching(String src, boolean isCJK) {
        if (src.contains("||")) src = src.split("\\|\\|", 2)[0];
        String[] stringSplit = src.split("\\|");
        StringBuilder result = new StringBuilder();

        for (final String stringSplitPart : stringSplit) {
            if (IGui.isCjk(stringSplitPart) == isCJK) {
                if (result.length() > 0) result.append(' ');
                result.append(stringSplitPart);
            }
        }
        return result.toString().trim();
    }
}
