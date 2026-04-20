package com.lx862.mtrscripting.core.util.video;

import java.awt.image.BufferedImage;

public interface Video {

    int totalFrames();

    Frame getFrame(int frame);

    BufferedImage extractFrameImage(int frameIndex);

    default VideoPlayback getPlayback(double targetFramerate) {
        return new VideoPlayback(this, 1000d / targetFramerate);
    }

    abstract class Frame {
        private final int frameNumber;

        public Frame(int frameNumber) {
            this.frameNumber = frameNumber;
        }
    }
}
