/**
 * MIT License
 *
 * Copyright (c) 2022-present Zbx1425
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.lx862.mtrscripting.mod.impl.mtr.util;

import org.mtr.mod.InitClient;
import org.mtr.mod.data.IGui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.lx862.jcm.mod.data.pids.preset.components.base.TextComponent.SWITCH_LANG_DURATION;

@SuppressWarnings("unused")
public class TextUtil {
    private TextUtil() {
    }

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
        String extraParts = getExtraMatching(src, true).trim();
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
            return extra ? "" : src;
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
