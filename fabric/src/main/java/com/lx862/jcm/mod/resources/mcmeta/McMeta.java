package com.lx862.jcm.mod.resources.mcmeta;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lx862.jcm.mod.data.Pair;

public class McMeta {
    /**
     * Tick elapsed since the last frame change
     */
    private int tickElapsed;
    private int defaultFrameTime = 1;
    private int verticalPart;
    private int currentPart = 0;
    public McMetaFrame[] frames;

    public static McMeta parse(String json) {
        McMeta mcMeta = new McMeta();
        JsonObject jsonObject =  new JsonParser().parse(json).getAsJsonObject();
        JsonObject animation = jsonObject.getAsJsonObject("animation");

        if(animation.has("frametime")) {
            mcMeta.setDefaultFrameTime(animation.get("frametime").getAsInt());
        }

        if(animation.has("frames")) {
            JsonArray customFrameOrderArray = animation.get("frames").getAsJsonArray();

            McMetaFrame[] customFrameOrder = new McMetaFrame[customFrameOrderArray.size()];
            for(int i = 0; i < customFrameOrderArray.size(); i++) {
                JsonElement frameData = customFrameOrderArray.get(i);
                if(frameData.isJsonObject()) {
                    JsonObject frameDataObject = frameData.getAsJsonObject();
                    int order = frameDataObject.get("index").getAsInt();
                    int duration = frameDataObject.has("time") ? frameDataObject.get("time").getAsInt() : mcMeta.defaultFrameTime;
                    customFrameOrder[i] = new McMetaFrame(order, duration);
                } else { // Porbably an int specifying the index
                    customFrameOrder[i] = new McMetaFrame(customFrameOrderArray.get(i).getAsInt(), mcMeta.defaultFrameTime);
                }
            }
            mcMeta.setFrames(customFrameOrder);
        }

        return mcMeta;
    }

    public void setFrames(McMetaFrame[] frames) {
        this.frames = frames;
    }

    public void setDefaultFrameTime(int defaultFrameTime) {
        this.defaultFrameTime = defaultFrameTime;
    }

    public Pair<Float, Float> getUV() {
        float onePart = 1F / verticalPart;
        int frameNow = frames[currentPart].getOrder();
        float startV = onePart * frameNow;
        return new Pair<>(startV, startV + onePart);
    }

    /**
     * This sets how much texture the image contains.<br>
     * If a custom frame order is not set beforehand, it will also set the frames in ascending order
     * @param part How much part vertically this texture contains
     */
    public void setVerticalPart(int part) {
        this.verticalPart = part;

        if(this.frames == null) {
            this.frames = new McMetaFrame[verticalPart];
            for(int i = 0; i < verticalPart; i++) {
                frames[i] = new McMetaFrame(i, defaultFrameTime);
            }
            setFrames(frames);
        }
    }

    public void tick() {
        if(tickElapsed >= frames[currentPart].getDuration()) {
            tickElapsed = 0;
            if(currentPart >= frames.length-1) {
                currentPart = 0;
            } else {
                currentPart++;
            }
        }

        tickElapsed++;
    }
}
