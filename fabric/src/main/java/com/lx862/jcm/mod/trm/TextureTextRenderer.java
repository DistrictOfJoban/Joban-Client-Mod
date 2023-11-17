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
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
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
    public static final int RENDERED_TEXT_SIZE = 10;
    private static final int DEFAULT_ATLAS_WIDTH = 1024;
    private static final int DEFAULT_ATLAS_HEIGHT = 1024;
    private static final int MAX_ATLAS_SIZE = 8192;
    public static final double MARQUEE_SPACING_RATIO = 0.8;
    private static final ObjectList<TextSlot> textSlots = new ObjectArrayList<>();
    private static NativeImageBackedTexture nativeImageBackedTexture = null;
    private static BufferedImage bufferedImageForTextGen = null;
    private static int width;
    private static int height;
    public static final int FONT_RESOLUTION = 64;
    public static Identifier textAtlas = null;

    public static void initialize() {
        initTextureAtlas(DEFAULT_ATLAS_WIDTH, DEFAULT_ATLAS_HEIGHT);
        JCMLogger.info("TextureRenderingManager Initialized.");
    }

    private static void initTextureAtlas(int width, int height) {
        textSlots.clear();
        TextureTextRenderer.width = width;
        TextureTextRenderer.height = height;
        NativeImage nativeImage = new NativeImage(width, height, false);
        nativeImage.fillRect(0, 0, width, height, 0xFF0000FF);

        if(bufferedImageForTextGen != null) {
            bufferedImageForTextGen.getGraphics().dispose();
        }
        if(nativeImageBackedTexture != null) {
            nativeImageBackedTexture.close();
        }

        nativeImageBackedTexture = new NativeImageBackedTexture(nativeImage);
        bufferedImageForTextGen = new BufferedImage(width, FONT_RESOLUTION, BufferedImage.TYPE_INT_ARGB);

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

    public static void addText(TextInfo text) {
        if(getTextSlot(text) == null) {
            Graphics2D graphics = bufferedImageForTextGen.createGraphics();
            graphics.setComposite(AlphaComposite.SrcOver);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setFont(new Font(text.getFont(), Font.PLAIN, FONT_RESOLUTION));

            AffineTransform affineTransform = new AffineTransform();
            AttributedString astr = getFormattedString(text, new Font(text.getFont(), Font.PLAIN, FONT_RESOLUTION));
            Rectangle2D fullTextBound = getTextBound(astr);

            if(text.isForScrollingText()) {
                affineTransform.scale((width / fullTextBound.getWidth()) * MARQUEE_SPACING_RATIO, 1);
            } else if(fullTextBound.getWidth() > width) {
                affineTransform.scale(width / fullTextBound.getWidth(), 1);
            }

            graphics.setTransform(affineTransform);
            graphics.drawString(astr.getIterator(), 0, graphics.getFontMetrics().getAscent() - (graphics.getFontMetrics().getDescent() / 2));

            findSlotAndDraw(bufferedImageForTextGen, graphics, text);

            // Clear for next use
            graphics.setComposite(AlphaComposite.Clear);
            graphics.fillRect(0, 0, width, bufferedImageForTextGen.getHeight());
            graphics.setComposite(AlphaComposite.SrcOver);
        }
    }

    private static AttributedString getFormattedString(TextInfo text, Font font) {
        String filteredString = MCTextHelper.removeColorCode(text.getContent());
        Int2IntArrayMap mcColorCodeMap = MCTextHelper.getColorCodeMap(text);

        AttributedString attributedString = new AttributedString(filteredString);
        attributedString.addAttribute(TextAttribute.FONT, font);
        attributedString.addAttribute(TextAttribute.FOREGROUND, text.getTextColor());

        int currentTextColor = text.getTextColor();
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

    private static void findSlotAndDraw(BufferedImage bufferedImage, Graphics2D graphics, TextInfo text) {
        boolean allUsedUp = textSlots.stream().noneMatch(TextSlot::unused);
        List<TextSlot> availableSlots = textSlots.stream().filter(e -> (allUsedUp ? e.canReuse() : e.unused())).sorted().collect(Collectors.toList());
        if(availableSlots.isEmpty()) {
            // We have absolutely no space left (Not even any reusable), probably a good idea to resize to a bigger image
            if(height + 1024 <= MAX_ATLAS_SIZE) {
                JCMLogger.debug("[TextureTextRenderer] No space left to draw text, resizing to a bigger atlas!");
                initTextureAtlas(width, Math.min(height + 1024, MAX_ATLAS_SIZE));
            }
            return;
        }

        TextSlot firstAvailableSlot = availableSlots.get(0);
        int textWidth = (int)getTextBound(text, graphics.getTransform()).getWidth();
        if(text.isForScrollingText()) textWidth = width;

        firstAvailableSlot.setContent(text, textWidth);
        drawToNativeImage(bufferedImage, firstAvailableSlot.getStartX(), firstAvailableSlot.getStartY(), firstAvailableSlot.getPixelWidth(), FONT_RESOLUTION);
    }

    private static void drawToNativeImage(BufferedImage bufferedImage, int x, int y, int width, int height) {
        for(int w = 0; w < width; w++) {
            for(int h = 0; h < height; h++) {
                if(w >= TextureTextRenderer.width || h >= TextureTextRenderer.height) continue;
                nativeImageBackedTexture.getImage().setPixelColor(x + w, y + h, bufferedImage.getRGB(w, h));
            }
        }

        if(nativeImageBackedTexture.getImage() != null) {
            // If you are looking for the reason why your text flickers, this is probably it
            // Uploading the entire image is too expensive, therefore we only update a portion of the image
            // But for reasons (I haven't looked deep yet), when rapidly generating images, sometimes it will show an older image before showing a new one
            // I hope a 8192px texture atlas is enough for you to work with~
            nativeImageBackedTexture.bindTexture();
            nativeImageBackedTexture.getImage().upload(0, x, y, x, y, width, height, false, false, false, false);
        }
    }

    private static TextSlot getTextSlot(TextInfo textInfo) {
        for(TextSlot textSlot : textSlots) {
            if(textSlot.isHoldingText(textInfo)) {
                return textSlot;
            }
        }
        return null;
    }

    public static void draw(GraphicsHolder graphicsHolder, TextInfo text, Direction facing, int x, int y, boolean centered) {
        TextSlot textSlot = getTextSlot(text);

        if(textSlot == null) {
            addText(text);
            textSlot = getTextSlot(text);
        }

        if(textSlot != null) {
            float finalX = centered ? x - (float)(textSlot.getPhysicalWidth() / 2F) : x;
            drawToWorld(graphicsHolder, textSlot, facing, finalX, y);
        }
    }

    private static void drawToWorld(GraphicsHolder graphicsHolder, TextSlot textSlot, Direction facing, float x, float y) {
        if(textSlot != null) {
            textSlot.updateLastAccessTime();
            float startY = textSlot.getStartY();
            float onePart = (float) FONT_RESOLUTION / height;

            float u1 = 0;
            float u2 = (float)textSlot.getPixelWidth() / width;
            float v1 = startY / height;
            float v2 = v1 + onePart;

            if(textSlot.getText().isForScrollingText()) {
                float ratio = textSlot.getMaxWidth() / (float)textSlot.getActualPhysicalWidth();
                u2 = u2 * ratio;

                u1 += (JCMStats.getGameTick() % 100) / 100F;
                u2 += (JCMStats.getGameTick() % 100) / 100F;
            }

            RenderHelper.drawTexture(graphicsHolder, x, y - 0.75F, 0, (int)textSlot.getPhysicalWidth(), RENDERED_TEXT_SIZE, u1, v1, u2, v2, facing, ARGB_WHITE, MAX_RENDER_LIGHT);
        }
    }

    public static void stressTest(int updateFrequency) {
        if(JCMStats.getGameTick() % updateFrequency == 0) {
            String[] strArr = new String[]{
                    "Nyaaaa", "610 Tuen Mun Ferry Pier", "The quick brown fox", "Hello World",
                    "Minecraft!", "meow meow", "Jumps Over the",
                    "lazy dog", "November", "December", "3 min", "Fuka",
                    "Drayton", "Joban Client Mod", "Minecraft Transit Railway", "Text Renderer",
                    "Block Entity", "SIGKILL", "Lorem ipsum",
                    "Drayton1", "Joban Client Mod1", "Minecraft Transit Railway1", "Text Renderer1",
                    "Block Entity!", "SIGKILL!", "Lorem ipsum!",
                    "lazy dog?", "November?", "December?", "3 min?", "Fuka?",};
            double colorRand = Math.random();
            String text = strArr[(int)(Math.random() * strArr.length)];

            if(colorRand > 0.5) {
                addText(new TextInfo(text).withColor(ARGB_BLACK));
            } else {
                addText(new TextInfo(text).withColor(ARGB_WHITE));
            }
        }
    }

    public static Rectangle2D getTextBound(TextInfo textInfo, AffineTransform affineTransform) {
        // TODO: Custom Font Loading
        AttributedString attributedString = getFormattedString(textInfo, new Font("Roboto", Font.PLAIN, FONT_RESOLUTION));
        return getTextBound(attributedString, affineTransform);
    }

    public static Rectangle2D getTextBound(AttributedString attributedString) {
        return getTextBound(attributedString, new AffineTransform());
    }

    public static Rectangle2D getTextBound(AttributedString attributedString, AffineTransform affineTransform) {
        FontRenderContext fontRenderContext = new FontRenderContext(affineTransform, true, true);
        TextLayout textLayout = new TextLayout(attributedString.getIterator(), fontRenderContext);
        return new Rectangle((int)textLayout.getBounds().getX(), (int)textLayout.getBounds().getY(), (int)(textLayout.getAdvance() * affineTransform.getScaleX()), (int)(textLayout.getBounds().getHeight() * affineTransform.getScaleY()));
    }

    public static int getPhysicalWidth(TextInfo textInfo) {
        TextSlot textSlot = getTextSlot(textInfo);
        if(textSlot != null) {
            return (int)textSlot.getPhysicalWidth();
        }
        return 0;
    }
}
