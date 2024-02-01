package com.lx862.jcm.mod.render.text;

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;

public interface MCTextHelper {

    /**
     * Get a hashmap of color code, with key representing the text position, and value being the integer color
     */
    static Int2IntArrayMap getColorCodeMap(TextInfo textInfo) {
        String text = textInfo.getContent();
        Int2IntArrayMap colorCodeMap = new Int2IntArrayMap();
        boolean hadSectionSign = false;

        if(!haveColorCode(text)) return colorCodeMap;

        int trueCharCount = 0;
        for(int i = 0; i < text.length(); i++) {
            char currentChar = text.charAt(i);
            if(hadSectionSign) {
                int newColorCode = Character.getNumericValue(currentChar);

                if(newColorCode != -1) {
                    int decodedColor = getColorCodeColor(newColorCode);

                    if(decodedColor != -1) colorCodeMap.put(trueCharCount, decodedColor);
                }

                if(currentChar == 'r') {
                    colorCodeMap.put(i, textInfo.getTextColor());
                }
                hadSectionSign = false;
            } else {
                hadSectionSign = currentChar == '§';
                if(!hadSectionSign) trueCharCount++;
            }
        }
        return colorCodeMap;
    }

    /**
     * Removes all color code (e.g. §0, §a ...) from the passed in String
     */
    static String removeColorCode(String text) {
        return !haveColorCode(text) ? text : text
                .replace("§0", "")
                .replace("§1", "")
                .replace("§2", "")
                .replace("§3", "")
                .replace("§4", "")
                .replace("§5", "")
                .replace("§6", "")
                .replace("§7", "")
                .replace("§8", "")
                .replace("§9", "")
                .replace("§a", "")
                .replace("§b", "")
                .replace("§c", "")
                .replace("§d", "")
                .replace("§e", "")
                .replace("§f", "")
                .replace("§r", "");
    }

    /**
     * Get the integer color of a Minecraft color code
     * @param code The numeric/hex code after the § character (e.g. §2 = 2)
     * @return The integer color, which can be converted to awt color with {@link java.awt.Color#Color(int)}
     */
    static int getColorCodeColor(int code) {
        switch(code) {
            case 0:
                return 0;
            case 1:
                return 0x0000AA;
            case 2:
                return 0x00AA00;
            case 3:
                return 0xAAAA00;
            case 4:
                return 0xAA0000;
            case 5:
                return 0xAA00AA;
            case 6:
                return 0xFFAA00;
            case 7:
                return 0xAAAAAA;
            case 8:
                return 0x555555;
            case 9:
                return 0x5555FF;
            case 10:
                return 0x55FF55;
            case 11:
                return 0x55FFFF;
            case 12:
                return 0xFF5555;
            case 13:
                return 0xFF55FF;
            case 14:
                return 0xFFFF55;
            case 15:
                return 0xFFFFFF;
            default:
                return -1;
        }
    }

    /**
     * Whether the input string contains Minecraft's color code (§).
     */
    static boolean haveColorCode(String str) {
        return str.contains("§");
    }
}
