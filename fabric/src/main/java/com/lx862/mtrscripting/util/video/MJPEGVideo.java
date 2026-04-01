package com.lx862.mtrscripting.util.video;

import java.util.List;

public class MJPEGVideo implements Video {
    private final byte[] data;
    private final List<Frame> frames;

    public MJPEGVideo(List<Frame> frames, byte[] data) {
        this.frames = frames;
        this.data = data;
    }

    @Override
    public int totalFrames() {
        return frames.size();
    }

    @Override
    public Frame getFrame(int frame) {
        if(frame >= frames.size()) return null;
        return frames.get(frame);
    }

    @Override
    public byte[] getData() {
        return data;
    }
}
