package com.lx862.mtrscripting.util.video;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public interface Video {

    int totalFrames();

    Frame getFrame(int frame);

    byte[] getData();

    default VideoPlayback getPlayback(double targetFramerate) {
        return new VideoPlayback(this, 1000d / targetFramerate);
    }

    default BufferedImage extractFrameImage(int frameIndex) {
        Frame frame = getFrame(frameIndex);
        try(ByteArrayInputStream bis = new ByteArrayInputStream(getData(), frame.startByte, frame.endByte - frame.startByte)) {
            return ImageIO.read(bis);
        } catch (IOException e) {
            return null;
        }
    }

    record Frame(int startByte, int endByte) {
    }
}
