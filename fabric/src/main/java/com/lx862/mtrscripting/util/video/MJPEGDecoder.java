package com.lx862.mtrscripting.util.video;

import java.util.ArrayList;
import java.util.List;

public class MJPEGDecoder implements VideoDecoder {
    @Override
    public Video decodeVideo(byte[] rawData) {
        List<Video.Frame> frames = new ArrayList<>();

        for (int i = 0; i < rawData.length - 1; i++) {
            // JPEG Start of Image marker: 0xFF 0xD8
            if ((rawData[i] & 0xFF) == 0xFF && (rawData[i + 1] & 0xFF) == 0xD8) {
                int start = i;
                int end = -1;

                // JPEG End of Image marker: 0xFF 0xD9
                for (int j = i + 1; j < rawData.length - 1; j++) {
                    if ((rawData[j] & 0xFF) == 0xFF && (rawData[j + 1] & 0xFF) == 0xD9) {
                        end = j + 2; // Include the marker itself
                        break;
                    }
                }

                if (end != -1) {
                    frames.add(new Video.Frame(start, end));
                    i = end - 1; // Skip to the end of frame
                }
            }
        }

        return new MJPEGVideo(frames, rawData);
    }
}
