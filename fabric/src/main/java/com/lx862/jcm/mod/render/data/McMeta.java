package com.lx862.jcm.mod.render.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.Pair;

public class McMeta {
    public static final int DEFAULT_FRAME_TIME = 1;
    private int tick;
    private int frameTime;
    private int totalDuration;
    private int verticalPart;
    public int[] frameOrder;

    public static McMeta parse(String json) {
        McMeta mcMeta = new McMeta();
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        JsonObject animation = jsonObject.getAsJsonObject("animation");
        int frameTime;
        if(animation.has("frametime")) {
            frameTime = animation.get("frametime").getAsInt();
        } else {
            frameTime = DEFAULT_FRAME_TIME;
        }
        mcMeta.setFrameTime(frameTime);

        if(animation.has("frames")) {
            JsonArray customFrameOrderArray = animation.get("frames").getAsJsonArray();
            int[] customFrameOrder = new int[customFrameOrderArray.size()];
            for(int i = 0; i < customFrameOrderArray.size(); i++) {
                JsonElement frameData = customFrameOrderArray.get(i);
                try {
                    JsonObject frameDataObject = frameData.getAsJsonObject();
                    // Too lazy to implement per frame duration :P
                    customFrameOrder[i] = frameDataObject.get("index").getAsInt();
                } catch (IllegalStateException e) {
                    customFrameOrder[i] = customFrameOrderArray.get(i).getAsInt();
                }
            }
            mcMeta.setFrameOrder(customFrameOrder);
        }

        return mcMeta;
    }

    public void setFrameOrder(int[] frameOrder) {
        this.frameOrder = frameOrder;
        this.totalDuration = frameOrder.length * frameTime;
    }

    public void setFrameTime(int frameTime) {
        this.frameTime = frameTime;
    }

    public Pair<Float, Float> getUV() {
        float onePart = 1F / verticalPart;
        int start = frameOrder[(tick / frameTime)];
        float startV = onePart * start;
        return new Pair<>(startV, startV + onePart);
    }

    /**
     * This sets how much texture the image contains.<br>
     * If a custom frame order is not set beforehand, it will also set the frames in ascending order
     * @param part How much part vertically this texture contains
     */
    public void setVerticalPart(int part) {
        this.verticalPart = part;

        if(this.frameOrder == null) {
            this.frameOrder = new int[verticalPart];
            for(int i = 0; i < verticalPart; i++) {
                frameOrder[i] = i;
            }
            setFrameOrder(frameOrder);
        }
    }

    public void tick() {
        if(tick+1 >= totalDuration) {
            tick = 0;
        } else {
            tick++;
        }
    }
}
