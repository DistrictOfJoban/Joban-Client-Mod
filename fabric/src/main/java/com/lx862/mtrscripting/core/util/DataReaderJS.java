package com.lx862.mtrscripting.core.util;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.annotation.ValueNullable;
import com.lx862.mtrscripting.mod.MTRScriptingModClient;
import com.lx862.mtrscripting.core.util.video.Video;
import com.lx862.mtrscripting.core.util.video.VideoDecoder;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DataReaderJS {
    public static final int CACHE_TIMEOUT_TICK = 20 * 5;
    private final InputStreamSupplier isSupplier;

    private final Cache<String> stringCache = new Cache<>();
    private final Cache<Font> fontCache = new Cache<>();
    private final Cache<BufferedImage> imageCache = new Cache<>();

    @ApiInternal
    public DataReaderJS(InputStreamSupplier isSupplier) {
        this.isSupplier = isSupplier;
    }

    public @ValueNullable String asString() {
        return cacheOrCreate(stringCache, () -> {
            try (InputStream is = asInputStream()) {
                return IOUtils.toString(is, StandardCharsets.UTF_8);
            } catch (Exception e) {
                MTRScriptingModClient.LOGGER.error("", e);
                return null;
            }
        });
    }

    public @ValueNullable BufferedImage asBufferedImage() {
        return cacheOrCreate(imageCache, () -> {
            try (InputStream is = asInputStream()) {
                BufferedImage image = ImageIO.read(is);
                return GraphicsTexture.createArgbBufferedImage(image);
            } catch (IOException e) {
                MTRScriptingModClient.LOGGER.error("[MTR Scripting via JCM] Failed to read image:", e);
                return null;
            }
        });
    }

    public @ValueNullable Font asFont() {
        return cacheOrCreate(fontCache, () -> {
            try(InputStream is = asInputStream()) {
                return Font.createFont(Font.TRUETYPE_FONT, is);
            } catch (Exception e) {
                MTRScriptingModClient.LOGGER.error("[MTR Scripting via JCM] Failed to read font:", e);
                return null;
            }
        });
    }

    public @ValueNullable byte[] asByteArray() {
        try (InputStream is = asInputStream()) {
            return is.readAllBytes();
        } catch (Exception e) {
            MTRScriptingModClient.LOGGER.error("[MTR Scripting via JCM] Failed to read raw bytes:", e);
            return null;
        }
    }

    public Video asVideo(String mimeType) {
        return VideoDecoder.decode(asByteArray(), mimeType);
    }

    public void openInputStream(Consumer<InputStream> inputStreamConsumer) throws IOException {
        try (InputStream is = asInputStream()) {
            inputStreamConsumer.accept(is);
        }
    }

    public InputStream asInputStream() throws IOException {
        return isSupplier.open();
    }

    @ApiInternal
    private <T> T cacheOrCreate(Cache<T> cacheStorage, Supplier<T> create) {
        if(cacheStorage.cached()) return cacheStorage.get();
        return cacheStorage.add(create.get());
    }

    @FunctionalInterface
    public interface InputStreamSupplier {
        InputStream open() throws IOException;
    }

    private static class Cache<T> {
        private T cacheMap = null;

        public boolean cached() {
            return cacheMap != null;
        }

        public T get() {
            return cacheMap;
        }

        public T add(T object) {
            cacheMap = object;
            return object;
        }

        public void reset() {
            this.cacheMap = null;
        }
    }
}
