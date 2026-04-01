package com.lx862.mtrscripting.util;

import com.lx862.jcm.mod.util.JCMLogger;
import com.lx862.mtrscripting.ScriptManager;
import com.lx862.mtrscripting.util.video.Video;
import com.lx862.mtrscripting.util.video.VideoDecoder;
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

    public DataReaderJS(InputStreamSupplier isSupplier) {
        this.isSupplier = isSupplier;
    }

    public String asString() {
        try (InputStream is = asInputStream()) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (Exception e) {
            JCMLogger.error("", e);
            return null;
        }
    }

    public BufferedImage asBufferedImage() {
        try (InputStream is = asInputStream()) {
            BufferedImage image = ImageIO.read(is);
            return GraphicsTexture.createArgbBufferedImage(image);
        } catch (IOException e) {
            ScriptManager.LOGGER.error("[JCM Scripting] Failed to read image:", e);
            return null;
        }
    }

    public Font asFont() {
        try(InputStream is = asInputStream()) {
            return Font.createFont(Font.TRUETYPE_FONT, is);
        } catch (Exception e) {
            ScriptManager.LOGGER.error("[JCM Scripting] Failed to read font:", e);
            return null;
        }
    }

    public byte[] asByteArray() {
        try (InputStream is = asInputStream()) {
            return is.readAllBytes();
        } catch (Exception e) {
            ScriptManager.LOGGER.error("[JCM Scripting] Failed to read raw bytes:", e);
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
