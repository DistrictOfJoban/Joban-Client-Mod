package com.lx862.jcm.mod.resources.mcmeta;

import com.lx862.jcm.mod.data.Pair;
import com.lx862.jcm.mod.util.JCMLogger;
import org.apache.commons.io.IOUtils;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.ResourceManagerHelper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.function.Consumer;

/**
 * Class to handle McMeta animated textures, as I couldn't figure out how MC does it :D
 */
public class McMetaManager {
    private static final HashMap<Identifier, McMeta> mcMetaList = new HashMap<>();

    /**
     * Try to load an animated mcmeta file if it exists, will do nothing otherwise
     * @param imagePath The identifier path that leads to the png file
     */
    public static void load(Identifier imagePath) {
        Identifier mcmetaFile = new Identifier(imagePath.getNamespace(), imagePath.getPath() + ".mcmeta");

        ResourceManagerHelper.readResource(mcmetaFile, inputStream -> {
            JCMLogger.info("Loading mcmeta file: " + imagePath.getPath());

            try {
                String str = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                McMeta mcMeta = McMeta.parse(str);

                readImage(mcMeta, imagePath, mcMeta1 -> {
                    mcMetaList.put(imagePath, mcMeta1);
                });
            } catch (IOException e) {
                e.printStackTrace();
                JCMLogger.warn("Cannot read mcmeta file {}!", imagePath.toString());
            }
        });
    }

    /**
     * Call to increment tick counter to update animated mcmeta texture.<br>
     * Only 1 party should call this at the end of each game tick.
     */
    public static void tick() {
        for(McMeta mcMeta : mcMetaList.values()) {
            mcMeta.tick();
        }
    }

    /**
     * The most vital method, it returns the start & end V for use when rendering with UV, so only 1 part of the image will be shown
     * @param id Identifier of the texture
     * @return The starting and the end V, default to 0.0 and 1.0 (Full Texture) if the texture is not a mcmeta animation loaded to this manager
     */
    public static Pair<Float, Float> getUV(Identifier id) {
        if(!mcMetaList.containsKey(id)) return new Pair<>(0F, 1F);
        return mcMetaList.get(id).getUV();
    }

    private static void readImage(McMeta mcMeta, Identifier imageFile, Consumer<McMeta> callback) {
        ResourceManagerHelper.readResource(imageFile, (inputStream -> {
            try {
                BufferedImage bufferedImage = ImageIO.read(inputStream);
                int width = bufferedImage.getWidth();
                int height = bufferedImage.getHeight();
                JCMLogger.info("Loaded mcmeta image metadata: {} ({})", imageFile.getPath(), width + "x" + height);
                mcMeta.setVerticalPart((height / width));
                callback.accept(mcMeta);
            } catch (IOException e) {
                JCMLogger.warn( "Failed to read mcmeta image file {}!", imageFile.getPath());
            }
        }));
    }
}
