package com.lx862.jcm.mod.resources.mcmeta;

public class McMetaFrame {
    private final int duration;
    private final int order;

    public McMetaFrame(int order, int duration) {
        if(duration < 1) {
            throw new IllegalArgumentException("Duration must be at least 1 or more!");
        } else if(order < 0) {
            throw new IllegalArgumentException("Index must not be less than 0!");
        }
        this.duration = duration;
        this.order = order;
    }

    public int getDuration() {
        return duration;
    }

    public int getOrder() {
        return order;
    }
}
