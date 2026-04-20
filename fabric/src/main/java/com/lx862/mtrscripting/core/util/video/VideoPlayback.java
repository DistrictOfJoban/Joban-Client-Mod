package com.lx862.mtrscripting.core.util.video;

import com.lx862.mtrscripting.core.annotation.ApiInternal;

import java.awt.image.BufferedImage;

public class VideoPlayback {
    private final Video video;
    private final double millisPerFrame;
    private long elapsedMillis = 0;
    private int currentFrameIndex = -1;
    private boolean isPaused = false;
    private BufferedImage currentFrame = null;

    @ApiInternal
    public VideoPlayback(Video video, double millisPerFrame) {
        this.video = video;
        this.millisPerFrame = millisPerFrame;
        update(0);
    }

    public void update(long elapsed) {
        if(!isPaused) elapsedMillis += elapsed;
        updateFrameImage();
    }

    public void seek(long elapsed) {
        elapsedMillis = elapsed;
        updateFrameImage();
    }

    public BufferedImage getCurrentFrame() {
        return currentFrame;
    }

    public void pause() {
        isPaused = true;
    }

    public void unpause() {
        isPaused = false;
    }

    @ApiInternal
    private void updateFrameImage() {
        int frameIndex = (int)Math.floor(elapsedMillis / millisPerFrame);
        if(frameIndex != currentFrameIndex) {
            if(frameIndex >= video.totalFrames()) return;
            currentFrame = video.extractFrameImage(frameIndex);
            currentFrameIndex = frameIndex;
        }
    }
}
