package com.lx862.jcm.mod.gui.widget;

import net.minecraft.client.gui.widget.Widget;
import org.mtr.mapping.holder.MutableText;

public class ListEntry {
    public final MutableText title;
    public final Widget widget;
    public final boolean isCategory;

    public ListEntry(MutableText title, Widget widget, boolean isCategory) {
        this.title = title;
        this.widget = widget;
        this.isCategory = isCategory;
    }

    public ListEntry(MutableText title, Widget widget) {
        this.title = title;
        this.widget = widget;
        this.isCategory = false;
    }
}
