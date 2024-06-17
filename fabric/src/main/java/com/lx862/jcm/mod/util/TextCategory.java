package com.lx862.jcm.mod.util;

public enum TextCategory {
    /** Block name */
    BLOCK("block"),
    /** Item name */
    ITEM("item"),
    /** In-game overlay, such as the action bar text */
    HUD("hud"),
    /** GUI Interfaces, usually element that are displayed in a screen user can interact with. */
    GUI("gui");

    final String prefix;

    TextCategory(String prefix) {
        this.prefix = prefix;
    }
}
