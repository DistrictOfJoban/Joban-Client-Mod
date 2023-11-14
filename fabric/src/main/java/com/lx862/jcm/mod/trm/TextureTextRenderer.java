package com.lx862.jcm.mod.trm;

import com.lx862.jcm.mod.data.JCMStats;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.GraphicsHolder;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Experimental text renderer using texture atlas
 */
public class TextureTextRenderer implements RenderHelper {
    private static final int ATLAS_WIDTH = 1024;
    private static final int ATLAS_HEIGHT = 1024;
    private static final int MAX_ATLAS_SIZE = 4096;
    private static final List<TextSlot> textSlots = new ArrayList<>();
    private static NativeImageBackedTexture nativeImageBackedTexture = null;
    private static BufferedImage bufferedImageForTextGen = null;
    public static int fontResolution = 64;
    public static Identifier textAtlas = null;

    public static void initialize() {
        if(bufferedImageForTextGen != null) {
            bufferedImageForTextGen.getGraphics().dispose();
        }
        bufferedImageForTextGen = new BufferedImage(fontResolution * 4, fontResolution, BufferedImage.TYPE_INT_ARGB);
        initTextureAtlas(ATLAS_WIDTH, ATLAS_HEIGHT);
        JCMLogger.info("TextureRenderingManager Initialized.");
    }

    private static void initTextureAtlas(int width, int height) {
        textSlots.clear();
        NativeImage nativeImage = new NativeImage(width, height, false);
        nativeImage.fillRect(0, 0, width, height, 0xFF0000FF);

        if(nativeImageBackedTexture != null) {
            nativeImageBackedTexture.close();
        }

        nativeImageBackedTexture = new NativeImageBackedTexture(nativeImage);

        if(textAtlas != null) {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(textAtlas);
        }
        textAtlas = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture("jcm_atlas_text", nativeImageBackedTexture);

        for(int i = 0; i < height / fontResolution; i++) {
            int startX = 0;
            int startY = i * fontResolution;
            textSlots.add(new TextSlot(startX, startY));
        }
    }

    public static void addText(String text, int color) {
        if(getTextSlot(text, color) == null) {
            Graphics2D graphics = bufferedImageForTextGen.createGraphics();
            graphics.setComposite(AlphaComposite.SrcOver);
            graphics.setColor(new Color(color));
            graphics.setFont(new Font("Arial", Font.PLAIN, fontResolution));
            int textWidth = graphics.getFontMetrics().stringWidth(text);

            if(textWidth > bufferedImageForTextGen.getWidth()) {
                resizeGenerationBufferImage(textWidth);
                addText(text, color);
                return;
            }

            graphics.drawString(text, 0, graphics.getFontMetrics().getAscent());
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            findSlotAndDraw(bufferedImageForTextGen, graphics, text, color);
            nativeImageBackedTexture.upload();

            // Clear for next use
            graphics.setComposite(AlphaComposite.Clear);
            graphics.fillRect(0, 0, bufferedImageForTextGen.getWidth(), bufferedImageForTextGen.getHeight());
            graphics.setComposite(AlphaComposite.SrcOver);
        }
    }

    public static void resizeGenerationBufferImage(int width) {
        bufferedImageForTextGen.getGraphics().dispose();
        bufferedImageForTextGen = new BufferedImage(width, bufferedImageForTextGen.getHeight(), BufferedImage.TYPE_INT_ARGB);
    }

    private static void findSlotAndDraw(BufferedImage bufferedImage, Graphics graphics, String text, int textColor) {
        boolean allUsedUp = textSlots.stream().noneMatch(TextSlot::unused);
        List<TextSlot> availableSlots = textSlots.stream().filter(e -> (allUsedUp ? e.canReuse() : e.unused())).sorted().collect(Collectors.toList());
        if(availableSlots.isEmpty()) {
            // We have absolutely no space left (Not even any reusable), probably a good idea to resize to a bigger image
            JCMLogger.debug("[TextureTextRenderer] No space left to render text, resizing to a bigger atlas!");
            initTextureAtlas(nativeImageBackedTexture.getImage().getWidth(), Math.min(nativeImageBackedTexture.getImage().getHeight() + 1024, MAX_ATLAS_SIZE));
            return;
        }

        TextSlot firstAvailableSlot = availableSlots.get(0);
        firstAvailableSlot.setText(text, graphics);
        firstAvailableSlot.setColor(textColor);

        TextSlot textSlot = firstAvailableSlot;
        drawToNativeImage(bufferedImage, textSlot.getStartX(), textSlot.getStartY(), textSlot.getWidth(), fontResolution);
    }

    private static void drawToNativeImage(BufferedImage bufferedImage, int x, int y, int width, int height) {
        for(int w = 0; w < width; w++) {
            for(int h = 0; h < height; h++) {
                if(w >= nativeImageBackedTexture.getImage().getWidth() || h >= nativeImageBackedTexture.getImage().getHeight()) continue;
                nativeImageBackedTexture.getImage().setPixelColor(x + w, y + h, bufferedImage.getRGB(w, h));
            }
        }
    }

    private static TextSlot getTextSlot(String text, int color) {
        for(TextSlot textSlot : textSlots) {
            if(textSlot != null && textSlot.equals(text, color)) {
                return textSlot;
            }
        }
        return null;
    }

    public static void draw(GraphicsHolder graphicsHolder, String text, Direction facing, int x, int y, int color) {
        TextSlot textSlot = getTextSlot(text, color);
        if(textSlot == null) {
            addText(text, color);
            textSlot = getTextSlot(text, color);
        }

        RenderHelper.drawTexture(graphicsHolder, x, y, 11 * textSlot.getScaledWidth(), 11, 0, facing, ARGB_WHITE, MAX_RENDER_LIGHT);
    }

    public static void stressTest(int updateFrequency) {
        if(JCMStats.getGameTick() % updateFrequency == 0) {
            String[] strArr = new String[]{"Nyaaaa", "610 Tuen Mun Ferry Pier", "The quick brown fox", "Hello World", "Minecraft!", "meow meow", "Jumps Over the", "lazy dog", "November", "December", "3 min", "Fuka", "Drayton", "Joban Client Mod", "Minecraft Transit Railway", "Text Renderer", "Block Entity", "SIGKILL", "Lorem ipsum"};
            double colorRand = Math.random();
            String text = strArr[(int)(Math.random() * strArr.length)];
            if(colorRand > 0.5) {
                addText(text, RenderHelper.ARGB_BLACK);
            } else {
                addText(text, RenderHelper.ARGB_WHITE);
            }
        }
    }
}
