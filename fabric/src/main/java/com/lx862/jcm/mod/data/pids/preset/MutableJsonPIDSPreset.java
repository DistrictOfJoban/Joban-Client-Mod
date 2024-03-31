package com.lx862.jcm.mod.data.pids.preset;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lx862.jcm.mod.render.TextOverflowMode;
import org.mtr.mapping.holder.Identifier;

import javax.annotation.Nullable;

public class MutableJsonPIDSPreset extends JsonPIDSPreset {
    public MutableJsonPIDSPreset(String id, @Nullable String name, @Nullable Identifier background, @Nullable String fontId, TextOverflowMode textOverflowMode, boolean[] rowHidden, boolean showClock, boolean showWeather, boolean topPadding, int textColor) {
        super(id, name, background, fontId, textOverflowMode, rowHidden, showClock, showWeather, topPadding, textColor);
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

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", id);
        jsonObject.addProperty("name", name == null ? id : name);
        jsonObject.addProperty("background", background.getNamespace() + ":" + background.getPath());
        jsonObject.addProperty("color", Integer.toHexString(textColor));
        jsonObject.addProperty("topPadding", topPadding);
        jsonObject.addProperty("showWeather", showWeather);
        jsonObject.addProperty("showClock", showClock);
        if(fontId != null) jsonObject.addProperty("fonts", fontId);

        JsonArray rowHidden = new JsonArray();
        for(int i = 0; i < 4; i++) {
            rowHidden.add(isRowHidden(i));
        }
        jsonObject.add("hideRow", rowHidden);
        return jsonObject;
    }
}
