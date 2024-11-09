package com.lx862.jcm.mod.render.text;

import com.lx862.jcm.mod.render.GuiHelper;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.render.text.font.FontSet;
import com.lx862.jcm.mod.util.JCMLogger;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;
import org.mtr.mod.InitClient;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.AttributedString;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <h2>Textured Text Renderer</h2>
 * <p>Draws a string to a texture atlas, which can then be drawn with 1 quad via UV selecting.</p>
 * <p>Under the vanilla rendering routine, this should offer substantial frame-rates improvement over the vanilla text renderer, at the cost of needing to constantly manage the atlas and being more memory intensive/less robust.</p>
 * <p>Call {@link TextRenderingManager#bind(GraphicsHolder)} to bind the texture, then call it's relevant draw method</p>
 */
public class TextureTextRenderer implements RenderHelper {
    /**
     * The size that will actually be rendered into the Minecraft world. (Final text size should be similar to vanilla's text rendering)
     */
    private static final int DEFAULT_ATLAS_WIDTH = 1024;
    private static final int DEFAULT_ATLAS_HEIGHT = 1024;
    private static final int MAX_ATLAS_SIZE = RenderSystem.maxSupportedTextureSize();
    private static final ObjectList<TextSlot> textSlots = new ObjectArrayList<>();
    private static NativeImageBackedTexture nativeImageBackedTexture = null;
    private static BufferedImage bufferedImageForTextGen = null;
    public static final int RENDERED_TEXT_SIZE = 9;
    public static final double MARQUEE_SPACING_RATIO = 0.8;
    public static final int FONT_RESOLUTION = 64;
    private static Identifier textAtlas = null;
    private static int width;
    private static int height;
    private static boolean initialized;

    public static void initialize() {
        if(initialized) close();
        JCMLogger.debug("Initializing TextureTextRenderer");
        initTextureAtlas(DEFAULT_ATLAS_WIDTH, DEFAULT_ATLAS_HEIGHT);
        initialized = true;
    }

    public static void close() {
        if(initialized) {
            nativeImageBackedTexture.close();
            nativeImageBackedTexture = null;
            bufferedImageForTextGen.flush();
            bufferedImageForTextGen = null;
            textSlots.clear();
            initialized = false;
        } else {
            throw new IllegalStateException("TextureTextRenderer already closed!");
        }
    }

    private static void ensureInitialized() {
        if(!initialized) initialize();
    }

    public static boolean initialized() {
        return initialized;
    }

    private static void initTextureAtlas(int width, int height) {
        textSlots.clear();
        TextureTextRenderer.width = width;
        TextureTextRenderer.height = height;
        NativeImage nativeImage = new NativeImage(width, height, false);
        // Red to highlight the background and easily distinguish it from anything else
        nativeImage.fillRect(0, 0, width, height, ARGB_RED);

        if(bufferedImageForTextGen != null) {
            bufferedImageForTextGen.getGraphics().dispose();
        }
        if(nativeImageBackedTexture != null) {
            nativeImageBackedTexture.close();
        }

        nativeImageBackedTexture = new NativeImageBackedTexture(nativeImage);
        bufferedImageForTextGen = new BufferedImage(width, FONT_RESOLUTION * 2, BufferedImage.TYPE_INT_ARGB);

        if(textAtlas != null) MinecraftClient.getInstance().getTextureManager().destroyTexture(textAtlas);
        textAtlas = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture("jcm_atlas_text", nativeImageBackedTexture);

        for(int i = 0; i < height / FONT_RESOLUTION; i++) {
            int startX = 0;
            int startY = i * FONT_RESOLUTION;
            textSlots.add(new TextSlot(startX, startY));
        }
    }

    public static int getAtlasWidth() {
        return initialized ? width : -1;
    }

    public static int getAtlasHeight() {
        return initialized ? height : -1;
    }

    public static void bindTexture(GraphicsHolder graphicsHolder) {
        ensureInitialized();
        graphicsHolder.createVertexConsumer(RenderLayer.getBeaconBeam(getAtlasIdentifier(), true));
    }

    public static void addText(TextInfo text) {
        ensureInitialized();

        if(getTextSlot(text) == null) {
            FontSet fontSet = text.getFontSet();
            Graphics2D graphics = bufferedImageForTextGen.createGraphics();
            graphics.setComposite(AlphaComposite.SrcOver);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            AffineTransform affineTransform = new AffineTransform();
            AttributedString attributedString = getFormattedString(text, fontSet);
            Rectangle2D fullTextBound = getTextBound(attributedString);

            if(text.isForScrollingText()) {
                affineTransform.scale((width / fullTextBound.getWidth()) * MARQUEE_SPACING_RATIO, 1);
            } else if(fullTextBound.getWidth() > width) {
                affineTransform.scale(width / fullTextBound.getWidth(), 1);
            }

            graphics.setTransform(affineTransform);

            GlyphVector gv = fontSet.getTallestGlyphVector(attributedString.getIterator(), graphics.getFontRenderContext(), FONT_RESOLUTION);
            Rectangle2D bound = gv.getOutline().getBounds();

            graphics.drawString(attributedString.getIterator(), 0, (int)bound.getHeight());
            drawOnFreeSlot(bufferedImageForTextGen, graphics, text);

            // Clear for next use
            graphics.setComposite(AlphaComposite.Clear);
            graphics.fillRect(0, 0, width, bufferedImageForTextGen.getHeight());
            graphics.setComposite(AlphaComposite.SrcOver);
        }
    }

    private static AttributedString getFormattedString(TextInfo text, FontSet fontSet) {
        String filteredString = MCTextHelper.removeColorCode(text.getContent());
        Int2IntArrayMap mcColorCodeMap = MCTextHelper.getColorCodeMap(text);

        AttributedString attributedString = new AttributedString(filteredString);
        if(filteredString.isEmpty()) return attributedString;

        Font primaryFont = fontSet.getPrimaryFont(FONT_RESOLUTION);
        attributedString.addAttribute(TextAttribute.FONT, primaryFont);
        attributedString.addAttribute(TextAttribute.FOREGROUND, text.getTextColor());

        int currentTextColor = text.getTextColor();
        attributedString = fontSet.getAttributedString(filteredString, attributedString, FONT_RESOLUTION);

        for(int i = 0; i < filteredString.length(); i++) {
            if(mcColorCodeMap.containsKey(i)) {
                currentTextColor = mcColorCodeMap.get(i);
            }

            if(currentTextColor != -1) {
                attributedString.addAttribute(TextAttribute.FOREGROUND, new Color(currentTextColor), i, i+1);
            }
        }
        return attributedString;
    }

    private static void drawOnFreeSlot(BufferedImage bufferedImage, Graphics2D graphics, TextInfo text) {
        ensureInitialized();

        boolean allUsedUp = textSlots.stream().noneMatch(TextSlot::unused);
        List<TextSlot> availableSlots = textSlots.stream().filter(e -> (allUsedUp ? e.reusable() : e.unused())).sorted().collect(Collectors.toList());
        if(availableSlots.isEmpty()) {
            // We have absolutely no space left (Not even any reusable), probably a good idea to resize to a bigger image
            if(height + 1024 <= MAX_ATLAS_SIZE) {
                JCMLogger.debug("[TextureTextRenderer] No space left to draw text, resizing to a bigger atlas!");
                initTextureAtlas(width, Math.min(height + 1024, MAX_ATLAS_SIZE));
            } else {
                JCMLogger.debug("[TextureTextRenderer] No space left to draw text, cannot resize beyond " + MAX_ATLAS_SIZE + "!");
            }
            return;
        }

        TextSlot firstAvailableSlot = availableSlots.get(0);
        Rectangle2D textBound = getTextBound(text, graphics.getTransform());
        int textWidth = (int)Math.ceil(textBound.getWidth());
        int textHeight = (int)Math.ceil(textBound.getHeight());
        if(text.isForScrollingText()) textWidth = width;

        firstAvailableSlot.setContent(text, textWidth, FONT_RESOLUTION);
        drawToNativeImage(bufferedImage, firstAvailableSlot.getStartX(), firstAvailableSlot.getStartY(), firstAvailableSlot.getPixelWidth(), FONT_RESOLUTION);
    }

    private static void drawToNativeImage(BufferedImage bufferedImage, int x, int y, int width, int height) {
        for(int w = 0; w < width; w++) {
            for(int h = 0; h < height; h++) {
                if(w >= TextureTextRenderer.width || h >= TextureTextRenderer.height) continue;
                nativeImageBackedTexture.getImage().setPixelColor(x + w, y + h, toAbgr(bufferedImage.getRGB(w, h)));
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

    public static int toAbgr(int rgb) {
        int a = (rgb >> 24) & 255;
        int r = (rgb >> 16) & 255;
        int g = (rgb >> 8) & 255;
        int b = (rgb) & 255;
        return a << 24 | b << 16 | g << 8 | r;
    }

    private static TextSlot getTextSlot(TextInfo textInfo) {
        for(TextSlot textSlot : textSlots) {
            if(textSlot.isHoldingText(textInfo)) {
                return textSlot;
            }
        }
        return null;
    }

    public static void draw(GraphicsHolder graphicsHolder, TextInfo text, Direction facing, double x, double y) {
        ensureInitialized();

        TextSlot textSlot = getTextSlot(text);

        if(textSlot == null) {
            addText(text);
            textSlot = getTextSlot(text);
        }

        if(textSlot != null) {
            float finalX = (float)text.getTextAlignment().getX(x, textSlot.getRenderedWidth());
            drawToWorld(graphicsHolder, textSlot, facing, finalX, (float)y);
        }
    }

    public static void draw(GuiDrawing guiDrawing, TextInfo text, double x, double y) {
        ensureInitialized();

        TextSlot textSlot = getTextSlot(text);

        if(textSlot == null) {
            addText(text);
            textSlot = getTextSlot(text);
        }

        if(textSlot != null) {
            float finalX = (float)text.getTextAlignment().getX(x, textSlot.getRenderedWidth());
            draw(guiDrawing, textSlot, finalX, (float)y);
        }
    }

    private static void draw(GuiDrawing guiDrawing, TextSlot textSlot, float x, float y) {
        ensureInitialized();

        textSlot.accessedNow();
        float startY = textSlot.getStartY();
        float onePart = (float) textSlot.getHeight() / height;

        float u1 = 0;
        float u2 = (float)textSlot.getPixelWidth() / width;
        float v1 = startY / height;
        float v2 = v1 + onePart;

        if(textSlot.getText().isForScrollingText()) {
            float ratio = textSlot.getMaxWidth() / (float)textSlot.getPhysicalWidth();
            u2 = u2 * ratio;

            u1 += (InitClient.getGameTick() % 100) / 100F;
            u2 += (InitClient.getGameTick() % 100) / 100F;
        }

        GuiHelper.drawTexture(guiDrawing, getAtlasIdentifier(), x, y, (int)textSlot.getRenderedWidth(), RENDERED_TEXT_SIZE, u1, v1, u2, v2);
    }

    private static void drawToWorld(GraphicsHolder graphicsHolder, TextSlot textSlot, Direction facing, float x, float y) {
        ensureInitialized();

        textSlot.accessedNow();
        float startY = textSlot.getStartY();
        float onePart = (float) textSlot.getHeight() / height;

        float u1 = 0;
        float u2 = (float)textSlot.getPixelWidth() / width;
        float v1 = startY / height;
        float v2 = v1 + onePart;

        if(textSlot.getText().isForScrollingText()) {
            float ratio = textSlot.getMaxWidth() / (float)textSlot.getPhysicalWidth();
            u2 = u2 * ratio;

            u1 += (InitClient.getGameTick() % 100) / 100F;
            u2 += (InitClient.getGameTick() % 100) / 100F;
        }

        RenderHelper.drawTexture(graphicsHolder, x, y - 0.75F, 0, (int)textSlot.getRenderedWidth(), RENDERED_TEXT_SIZE, u1, v1, u2, v2, facing, ARGB_WHITE, MAX_RENDER_LIGHT);
    }

    public static void stressTest(int updateFrequency) {
        if(InitClient.getGameTick() % updateFrequency == 0) {
            String[] strArr = new String[]{
                    "Central", "610 Tuen Mun Ferry Pier", "Admiralty", "Kennedy Town",
                    "Minecraft!", "$050302", "33:44",
                    "October", "November", "December", "3 min", "^_^",
                    "Fabric modloader", "Joban Client Mod", "Minecraft Transit Railway 3", "Text Renderer",
                    "Block Entity", "SIGKILL", "Lorem ipsum",
                    "Minceraft", "JCM", "Minecraft Transit Railway 4", "Text Rendering",
                    "Block Entity Renderer", "main", "Fish.",
                    "The quick brown fox jumps over the lazy dog", "woem!", "git", "Stress Test In Progress", "[07L]",};
            double colorRand = Math.random();
            String text = strArr[(int)(Math.random() * strArr.length)];

            if(colorRand > 0.5) {
                addText(new TextInfo(text).withColor(ARGB_BLACK));
            } else {
                addText(new TextInfo(text).withColor(ARGB_WHITE));
            }
        }
    }

    public static Rectangle2D getTextBound(TextInfo textInfo) {
        return getTextBound(textInfo, new AffineTransform());
    }

    public static Rectangle2D getTextBound(TextInfo textInfo, AffineTransform affineTransform) {
        AttributedString attributedString = getFormattedString(textInfo, textInfo.getFontSet());
        return getTextBound(attributedString, affineTransform);
    }

    public static Rectangle2D getTextBound(AttributedString attributedString) {
        return getTextBound(attributedString, new AffineTransform());
    }

    /**
     * Obtain a rectangular region of a string in pixel
     * @return The rectangular bound of the string provided
     */
    public static Rectangle2D getTextBound(AttributedString attributedString, AffineTransform affineTransform) {
        if(attributedString.getIterator().getBeginIndex() == attributedString.getIterator().getEndIndex()) return new Rectangle();

        FontRenderContext fontRenderContext = new FontRenderContext(affineTransform, true, true);
        TextLayout textLayout = new TextLayout(attributedString.getIterator(), fontRenderContext);
        return new Rectangle((int)textLayout.getBounds().getX(), (int)textLayout.getBounds().getY(), (int)(textLayout.getAdvance() * affineTransform.getScaleX()), (int)(textLayout.getBounds().getHeight() * affineTransform.getScaleY()));
    }

    /**
     * Obtain the width of the corresponding TextInfo.
     * @param textInfo The textInfo with the same configuration as drawn (Where the text content, color, forScrollingText and font must match)
     * @return The in-game width of the TextInfo.
     */
    public static int getPhysicalWidth(TextInfo textInfo) {
        double pixelWidth = Math.min(DEFAULT_ATLAS_WIDTH, getTextBound(textInfo).getWidth());
        double physicalWidth = (pixelWidth / TextureTextRenderer.FONT_RESOLUTION) * TextureTextRenderer.RENDERED_TEXT_SIZE;

        return (int)textInfo.getWidthInfo().clampWidth(physicalWidth);
    }

    public static Identifier getAtlasIdentifier() {
        return textAtlas;
    }
}
