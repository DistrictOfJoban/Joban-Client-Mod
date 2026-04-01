package com.lx862.mtrscripting.util.video;

public interface VideoDecoder {
    Video decodeVideo(byte[] rawData);

    static Video decode(byte[] rawData, String mimeType) {
        switch(mimeType) {
            case "video/x-motion-jpeg":
                return new MJPEGDecoder().decodeVideo(rawData);
            default:
                throw new IllegalArgumentException("Unknown video format " + mimeType);
        }
    }
}
