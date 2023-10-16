package com.lx862.jcm.mod.util;

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
