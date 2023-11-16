package com.lx862.jcm.mod.trm;

import com.lx862.jcm.mod.data.JCMStats;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.util.JCMLogger;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.GraphicsHolder;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.text.AttributedString;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Experimental text renderer using texture atlas
 */
public class TextureTextRenderer implements RenderHelper {
    /**
     * The size that will actually be rendered into the Minecraft world. (Final text size should be similar to vanilla's text rendering)
     */
    private static final int RENDERED_TEXT_SIZE = 10;
    private static final int DEFAULT_ATLAS_WIDTH = 1024;
    private static final int DEFAULT_ATLAS_HEIGHT = 1024;
    private static final int MAX_ATLAS_SIZE = 4096;
    private static final ObjectList<TextSlot> textSlots = new ObjectArrayList<>();
    private static NativeImageBackedTexture nativeImageBackedTexture = null;
    private static BufferedImage bufferedImageForTextGen = null;
    private static int width;
    private static int height;
    public static final int FONT_RESOLUTION = 64;
    public static Identifier textAtlas = null;

    public static void initialize() {
        if(bufferedImageForTextGen != null) {
            bufferedImageForTextGen.getGraphics().dispose();
        }
        bufferedImageForTextGen = new BufferedImage(FONT_RESOLUTION * 4, FONT_RESOLUTION, BufferedImage.TYPE_INT_ARGB);
        initTextureAtlas(DEFAULT_ATLAS_WIDTH, DEFAULT_ATLAS_HEIGHT);
        JCMLogger.info("TextureRenderingManager Initialized.");
    }

    private static void initTextureAtlas(int width, int height) {
        textSlots.clear();
        TextureTextRenderer.width = width;
        TextureTextRenderer.height = height;
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

        for(int i = 0; i < height / FONT_RESOLUTION; i++) {
            int startX = 0;
            int startY = i * FONT_RESOLUTION;
            textSlots.add(new TextSlot(startX, startY));
        }
    }

    public static void addText(String text, int color) {
        if(getTextSlot(text, color) == null) {
            Graphics2D graphics = bufferedImageForTextGen.createGraphics();
            graphics.setComposite(AlphaComposite.SrcOver);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Font font = new Font("Roboto", Font.PLAIN, FONT_RESOLUTION);

            graphics.setColor(new Color(color));
            graphics.setFont(font);
            int textWidth = graphics.getFontMetrics().stringWidth(text);

            if(textWidth > bufferedImageForTextGen.getWidth()) {
                resizeGenerationBufferImage(textWidth);
                addText(text, color);
                return;
            }

            AttributedString astr = getFormattedString(text, font);
            graphics.drawString(astr.getIterator(), 0, graphics.getFontMetrics().getAscent() - (graphics.getFontMetrics().getDescent() / 2));

            findSlotAndDraw(bufferedImageForTextGen, graphics, text, color);
            nativeImageBackedTexture.upload();

            // Clear for next use
            graphics.setComposite(AlphaComposite.Clear);
            graphics.fillRect(0, 0, bufferedImageForTextGen.getWidth(), bufferedImageForTextGen.getHeight());
            graphics.setComposite(AlphaComposite.SrcOver);
        }
    }

    private static AttributedString getFormattedString(String text, Font font) {
        String filteredString = MCTextHelper.removeColorCode(text);
        Int2IntArrayMap mcColorCodeMap = MCTextHelper.getColorCodeMap(text);

        AttributedString attributedString = new AttributedString(filteredString);
        attributedString.addAttribute(TextAttribute.FONT, font);

        int currentTextColor = -1;
        // Change font of individual characters so they still get rendered with a fallback font
        for(int i = 0; i < filteredString.length(); i++) {
            char currentChar = filteredString.charAt(i);

            if(!font.canDisplay(currentChar)) {
                for(Font sysFont : GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts()) {
                    if(sysFont.canDisplay(filteredString.charAt(i))) {
                        // FIXME: Temp Blacklist for local wacky font
                        if(sysFont.getFontName().startsWith("Casey")) continue;
                        attributedString.addAttribute(TextAttribute.FONT, sysFont.deriveFont(Font.PLAIN, FONT_RESOLUTION), i, i+1);
                        break;
                    }
                }
            }

            if(mcColorCodeMap.containsKey(i)) {
                currentTextColor = mcColorCodeMap.get(i);
            }

            if(currentTextColor != -1) {
                attributedString.addAttribute(TextAttribute.FOREGROUND, new Color(currentTextColor), i, i+1);
            }
        }
        return attributedString;
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
            initTextureAtlas(width, Math.min(height + 1024, MAX_ATLAS_SIZE));
            return;
        }

        TextSlot firstAvailableSlot = availableSlots.get(0);
        firstAvailableSlot.setText(text, graphics);
        firstAvailableSlot.setColor(textColor);

        TextSlot textSlot = firstAvailableSlot;
        drawToNativeImage(bufferedImage, textSlot.getStartX(), textSlot.getStartY(), textSlot.getWidth(), FONT_RESOLUTION);
    }

    private static void drawToNativeImage(BufferedImage bufferedImage, int x, int y, int width, int height) {
        for(int w = 0; w < width; w++) {
            for(int h = 0; h < height; h++) {
                if(w >= TextureTextRenderer.width || h >= TextureTextRenderer.height) continue;
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

    public static void draw(GraphicsHolder graphicsHolder, String text, Direction facing, int x, int y, int color, boolean centered) {
        TextSlot textSlot = getTextSlot(text, color);
        if(textSlot == null) {
            addText(text, color);
            textSlot = getTextSlot(text, color);
        }

        if(textSlot != null) {
            float finalX = centered ? x - (float)((RENDERED_TEXT_SIZE * textSlot.getScaledWidth()) / 2F) : x;
            drawToWorld(graphicsHolder, textSlot, facing, finalX, y);
        }
    }

    private static void drawToWorld(GraphicsHolder graphicsHolder, TextSlot textSlot, Direction facing, float x, float y) {
        if(textSlot != null) {
            textSlot.updateLastAccessTime();
            float startY = textSlot.getStartY();
            float onePart = (float) FONT_RESOLUTION / height;

            float u1 = 0;
            float u2 = (float)textSlot.getWidth() / width;
            float v1 = startY / height;
            float v2 = v1 + onePart;

            RenderHelper.drawTexture(graphicsHolder, x, y - 0.75F, 0, (int)(RENDERED_TEXT_SIZE * textSlot.getScaledWidth()), RENDERED_TEXT_SIZE, u1, v1, u2, v2, facing, ARGB_WHITE, MAX_RENDER_LIGHT);
        }
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
