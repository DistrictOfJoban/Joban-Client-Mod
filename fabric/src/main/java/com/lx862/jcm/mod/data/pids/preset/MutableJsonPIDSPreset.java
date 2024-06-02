package com.lx862.jcm.mod.data.pids.preset;

import com.lx862.jcm.mod.render.TextOverflowMode;
import org.mtr.mapping.holder.Identifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MutableJsonPIDSPreset extends JsonPIDSPreset {
    public MutableJsonPIDSPreset(String id, @Nullable String name, @Nonnull Identifier background, @Nullable String fontId, TextOverflowMode textOverflowMode, boolean[] rowHidden, boolean showClock, boolean showWeather, boolean topPadding, int textColor) {
        super(id, name, background, fontId, textOverflowMode, rowHidden, showClock, showWeather, topPadding, textColor);
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.id = name;
    }

    public void setShowWeather(boolean newShowWeather) {
        showWeather = newShowWeather;
    }

    public void setShowClock(boolean newShowClock) {
        showClock = newShowClock;
    }

    public void setBackground(Identifier newBackground) {
        background = newBackground;
    }

    public void setTopPadding(boolean newTopPadding) {
        topPadding = newTopPadding;
    }

    public void setColor(int newColor) {
        textColor = newColor;
    }

    public void setRowHidden(int i, boolean bool) {
        rowHidden[i] = bool;
    }
}