package com.lx862.mtrscripting.core.util.video;

import com.lx862.mtrscripting.core.annotation.ApiInternal;

public interface VideoDecoder {
    @ApiInternal
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
