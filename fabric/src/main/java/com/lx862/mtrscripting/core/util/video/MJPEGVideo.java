package com.lx862.mtrscripting.core.util.video;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
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
    public BufferedImage extractFrameImage(int frameIndex) {
        Frame frame = getFrame(frameIndex);
        try(ByteArrayInputStream bis = new ByteArrayInputStream(data, frame.startByte, frame.endByte - frame.startByte)) {
            return ImageIO.read(bis);
        } catch (IOException e) {
            return null;
        }
    }

    public static class Frame extends Video.Frame {
        public final int startByte;
        public final int endByte;

        public Frame(int frameNumber, int startByte, int endByte) {
            super(frameNumber);
            this.startByte = startByte;
            this.endByte = endByte;
        }
    }
}
