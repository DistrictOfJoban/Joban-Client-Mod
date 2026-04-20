package com.lx862.mtrscripting.core.util;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.annotation.ValueNullable;
import com.lx862.mtrscripting.mod.MTRScriptingMod;
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

public class DataReaderJS {
    private final InputStreamSupplier isSupplier;

    @ApiInternal
    public DataReaderJS(InputStreamSupplier isSupplier) {
        this.isSupplier = isSupplier;
    }

    public @ValueNullable String asString() {
        try (InputStream is = asInputStream()) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (Exception e) {
            MTRScriptingMod.LOGGER.error("", e);
            return null;
        }
    }

    public @ValueNullable BufferedImage asBufferedImage() {
        try (InputStream is = asInputStream()) {
            BufferedImage image = ImageIO.read(is);
            return GraphicsTexture.createArgbBufferedImage(image);
        } catch (IOException e) {
            MTRScriptingMod.LOGGER.error("[MTR Scripting via JCM] Failed to read image:", e);
            return null;
        }
    }

    public @ValueNullable Font asFont() {
        try(InputStream is = asInputStream()) {
            return Font.createFont(Font.TRUETYPE_FONT, is);
        } catch (Exception e) {
            MTRScriptingMod.LOGGER.error("[MTR Scripting via JCM] Failed to read font:", e);
            return null;
        }
    }

    public @ValueNullable byte[] asByteArray() {
        try (InputStream is = asInputStream()) {
            return is.readAllBytes();
        } catch (Exception e) {
            MTRScriptingMod.LOGGER.error("[MTR Scripting via JCM] Failed to read raw bytes:", e);
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

    @FunctionalInterface
    public interface InputStreamSupplier {
        InputStream open() throws IOException;
    }
}
