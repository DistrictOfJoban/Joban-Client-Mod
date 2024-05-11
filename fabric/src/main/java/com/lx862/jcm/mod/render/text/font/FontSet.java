package com.lx862.jcm.mod.render.text.font;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.ResourceManagerHelper;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.TextAttribute;
import java.awt.geom.Rectangle2D;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * An object that contains a list of font. This is the equivalent of a Minecraft Font json, where a list of font may be defined here.
 */
public class FontSet {
    private final List<Font> fontList;

    public FontSet(Font... fonts) {
        fontList = new ArrayList<>();

        for(int i = 0; i < fonts.length; i++) {
            fontList.add(fonts[i]);
        }
    }

    public FontSet(JsonObject vanillaFontJson) throws NoTTFFontException {
        this();
        JsonArray fonts = vanillaFontJson.getAsJsonArray("providers");

        boolean hasTTFFont = false;
        for(int i = 0; i < fonts.size(); i++) {
            JsonObject fontJson = fonts.get(i).getAsJsonObject();
            String type = fontJson.get("type").getAsString();
            if(!type.equals("ttf")) continue;
            hasTTFFont = true;

            String fontFile = fontJson.get("file").getAsString();
            Identifier fontFileId = new Identifier(fontFile);
            String fontFilePath = "font/" + fontFileId.getPath();
            readFontFile(new Identifier(fontFileId.getNamespace(), fontFilePath), fontList::add);
        }

        if(!hasTTFFont) {
            throw new NoTTFFontException();
        }
    }

    private void readFontFile(Identifier path, Consumer<Font> callback) {
        ResourceManagerHelper.readResource(path, inputStream -> {
            try {
                Font createdFont = Font.createFont(Font.TRUETYPE_FONT, inputStream);
                callback.accept(createdFont);
            } catch (Exception e) {
                JCMLogger.warn("Failed to load font from path " + path.getPath());
                e.printStackTrace();
            }
        });
    }

    /**
     * Format an attributed string with font that is suited for each character. The available font depends on the font set. (e.g. A chinese font would be used on chinese characters)
     */
    public AttributedString getAttributedString(String string, AttributedString attributedString, int fontResolution) {
        for(int i = 0; i < string.length(); i++) {
            for(Font font : fontList) {
                if(font.canDisplay(string.charAt(i))) {
                    attributedString.addAttribute(TextAttribute.FONT, font.deriveFont(Font.PLAIN, fontResolution), i, i+1);
                    break;
                }
            }
        }
        return attributedString;
    }

    /**
     * Get the glyph vector object of the font with the tallest glyph
     * @param iterator Obtained from AttributedString.getIterator()
     * @param frc Font Rendering Context
     */
    public GlyphVector getTallestGlyphVector(AttributedCharacterIterator iterator, FontRenderContext frc, int fontSize) {
        Font largestFont = null;
        GlyphVector largestGv = null;
        double largestHeight = 0;

        while(iterator.getIndex() < iterator.getEndIndex()) {
            Font font = (Font)iterator.getAttribute(TextAttribute.FONT);
            if(font != largestFont) {
                GlyphVector gv = font.deriveFont(Font.PLAIN, fontSize).createGlyphVector(frc, "a!1b");
                Rectangle2D bound = gv.getOutline().getBounds();
                double height = bound.getHeight();
                if(height > largestHeight) {
                    largestHeight = height;
                    largestGv = gv;
                    largestFont = font;
                }
            }

            iterator.next();
        }

        return largestGv;
    }

    public Font getPrimaryFont(int fontSize) {
        return fontList.get(0).deriveFont(fontSize);
    }
}
