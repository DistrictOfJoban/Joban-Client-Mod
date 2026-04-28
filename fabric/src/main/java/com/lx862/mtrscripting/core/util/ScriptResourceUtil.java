/**
 * MIT License
 *
 * Copyright (c) 2022-present Zbx1425
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.lx862.mtrscripting.core.util;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.annotation.ValueNullable;
import com.lx862.mtrscripting.core.api.MTRScriptingAPI;

import com.lx862.mtrscripting.lib.org.mozilla.javascript.Context;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.Scriptable;
import com.lx862.mtrscripting.mod.MTRScriptingMod;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.ResourceManagerHelper;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.text.AttributedString;
import java.util.Locale;
import java.util.Stack;

@SuppressWarnings("unused")
public class ScriptResourceUtil {
    private static final Stack<Identifier> scriptLocationStack = new Stack<>();
    private static Context activeContext;
    private static Scriptable activeScope;

    private static final FontRenderContext FONT_CONTEXT = new FontRenderContext(new AffineTransform(), true, false);
    private static final Identifier NOTO_SANS_CJK_LOCATION = new Identifier("mtr", "font/noto-sans-cjk-tc-medium.otf");
    private static final Identifier NOTO_SANS_LOCATION = new Identifier("mtr", "font/noto-sans-semibold.ttf");
    private static final Identifier NOTO_SERIF_LOCATION = new Identifier("mtr", "font/noto-serif-cjk-tc-semibold.ttf");
    private static Font NOTO_SANS_CACHE;
    private static Font NOTO_SANS_CJK_CACHE;
    private static Font NOTO_SERIF_CACHE;

    @ApiInternal
    public static void beginParseScript(Context context, Scriptable scope) {
        if(activeContext != null || activeScope != null) throw new IllegalStateException("Must call finishParseScript() before beginParseScript()!");
        activeContext = context;
        activeScope = scope;
    }

    @ApiInternal
    public static void finishParseScript() {
        activeContext = null;
        activeScope = null;
    }

    @ApiInternal
    public static void executeScript(Context rhinoCtx, Scriptable scope, Identifier scriptLocation, String script) {
        scriptLocationStack.push(scriptLocation);
        rhinoCtx.evaluateString(scope, script, scriptLocation.getNamespace() + ":" + scriptLocation.getPath(), 1, null);
        scriptLocationStack.pop();
    }

    public static void includeScript(Object pathOrIdentifier) {
        if (activeContext == null) throw new RuntimeException(
            "Cannot use include() in Runtime Stage, as all scripts has been parsed and finalized at this point!"
        );
        Identifier identifier;
        if (pathOrIdentifier instanceof Identifier) {
            identifier = (Identifier) pathOrIdentifier;
        } else {
            identifier = idRelative(pathOrIdentifier.toString());
        }
        executeScript(activeContext, activeScope, identifier, ResourceManagerHelper.readResource(identifier));
    }

    public static void print(@ValueNullable Object... objs) {
        StringBuilder sb = new StringBuilder();
        if(objs == null) {
            sb.append("null");
        } else {
            for(Object obj : objs) {
                sb.append(obj);
            }
        }
        ConsoleJS.log(sb.toString());
    }

    @Deprecated
    public static Object manager() {
        return null;
    }

    public static Identifier identifier(String textForm) {
        return id(textForm);
    }

    public static Identifier id(String textForm) {
        return new Identifier(textForm);
    }

    public static Identifier idRelative(String textForm) {
        return idr(textForm);
    }

    public static Identifier idr(String textForm) {
        if (scriptLocationStack.empty()) throw new RuntimeException(
                "Cannot use idr/idRelative in functions."
        );
        Identifier id = scriptLocationStack.peek();
        return resolveRelativePath(id, textForm);
    }

    @Deprecated
    public static @ValueNullable InputStream readStream(Identifier identifier) throws IOException {
        DataReaderJS dataReader = read(identifier);
        if(dataReader != null) {
            return dataReader.asInputStream();
        } else {
            return null;
        }
    }

    @Deprecated
    public static @ValueNullable String readString(Identifier identifier) {
        DataReaderJS dataReader = read(identifier);
        if(dataReader == null) return null;
        return dataReader.asString();
    }

    @Deprecated
    public static @ValueNullable BufferedImage readBufferedImage(Identifier id) {
        DataReaderJS dataReader = read(id);
        if(dataReader == null) return null;
        return dataReader.asBufferedImage();
    }

    @Deprecated
    public static @ValueNullable Font readFont(Identifier id) {
        DataReaderJS dataReader = read(id);
        if(dataReader == null) return null;
        return dataReader.asFont();
    }

    public static @ValueNullable DataReaderJS read(Identifier identifier) {
        // HACK: MC Mappings always uses a callback for the InputStream and auto-closes it after, so we can't pass it around later on
        // For now, we just save the full file bytes, then wrap it in another ByteArrayInputStream...
        byte[][] fileBytes = new byte[][]{null};
        ResourceManagerHelper.readResource(identifier, is -> {
            try {
                fileBytes[0] = is.readAllBytes();
            } catch (IOException e) {
                MTRScriptingMod.LOGGER.error("[JCM Scripting] Error while reading data {}", identifier);
            }
        });

        if(fileBytes[0] != null) {
            return new DataReaderJS(() -> new ByteArrayInputStream(fileBytes[0]));
        } else {
            return null;
        }
    }

    public static boolean exist(Identifier id) {
        boolean[] resourceFound = new boolean[]{false};
        ResourceManagerHelper.readResource(id, is -> resourceFound[0] = true);
        return resourceFound[0];
    }

    public static Font getSystemFont(String fontName) {
        if(fontName.equals("Noto Sans")) {
            Font cjkFont = getSystemFont("Noto Sans CJK TC Medium");
            if (cjkFont == null) {
                return getSystemFont("Noto Sans SemiBold");
            }
            return cjkFont;
        } else if(fontName.equals("Noto Serif")) {
            if(NOTO_SERIF_CACHE == null) {
                try {
                    NOTO_SERIF_CACHE = readFont(NOTO_SERIF_LOCATION);
                } catch (Exception ex) {
                    MTRScriptingMod.LOGGER.error("[JCM Scripting] Failed loading font", ex);
                    return null;
                }
            }
            return NOTO_SERIF_CACHE;
        } else if(fontName.equals("Noto Sans CJK TC Medium")) {
            if(NOTO_SANS_CJK_CACHE == null) {
                try {
                    NOTO_SANS_CJK_CACHE = readFont(NOTO_SANS_CJK_LOCATION);
                } catch (Exception ex) {
                    MTRScriptingMod.LOGGER.error("[JCM Scripting] Failed to load font", ex);
                }
            }
            return NOTO_SANS_CJK_CACHE;
        } else if(fontName.equals("Noto Sans SemiBold")) {
            if(NOTO_SANS_CACHE == null) {
                try {
                    NOTO_SANS_CACHE = readFont(NOTO_SANS_LOCATION);
                } catch (Exception ex) {
                    MTRScriptingMod.LOGGER.error("[JCM Scripting] Failed to load font", ex);
                }
            }
            return NOTO_SANS_CACHE;
        } else {
            return new Font(fontName, Font.PLAIN, 1);
        }
    }

    public static FontRenderContext getFontRenderContext() {
        return FONT_CONTEXT;
    }

    public static AttributedString ensureStrFonts(String text, Font primaryFont, Font... fallbackFonts) {
        AttributedString result = new AttributedString(text);
        if (text.isEmpty()) return result;
        result.addAttribute(TextAttribute.FONT, primaryFont, 0, text.length());
        for (int characterIndex = 0; characterIndex < text.length(); characterIndex++) {
            final char character = text.charAt(characterIndex);
            if (!primaryFont.canDisplay(character)) {
                Font defaultFont = null;
                for(final Font testFont : fallbackFonts) {
                    if (testFont.canDisplay(character)) {
                        defaultFont = testFont;
                        break;
                    }
                }
                // Still no hope, find all system fonts...
                for (final Font testFont : GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts()) {
                    if (testFont.canDisplay(character)) {
                        defaultFont = testFont;
                        break;
                    }
                }
                final Font newFont = (defaultFont == null ? new Font(null) : defaultFont).deriveFont(primaryFont.getStyle(), primaryFont.getSize2D());
                result.addAttribute(TextAttribute.FONT, newFont, characterIndex, characterIndex + 1);
            }
        }
        return result;
    }

    public static String getAddonVersion(String modId) {
        return MTRScriptingAPI.getAddonVersion(modId);
    }

    @Deprecated
    public static String getMTRVersion() {
        return getAddonVersion("mtr"); // This assumes MTR have registered the version
    }

    @Deprecated
    public static String getNTEVersion() {
        // TODO: Return the real Minecraft version?
        return "0.5.2+1.19.2"; // Hardcoded for backward compat
    }

    @Deprecated
    public static int getNTEVersionInt() {
        return 502; // Hardcoded for backward compat
    }

    @Deprecated
    public static int getNTEProtoVersion() {
        return 2; // Hardcoded for backward compat
    }

    private static Identifier resolveRelativePath(Identifier baseFile, String relative) {
        String result = relative.toLowerCase(Locale.ROOT).replace('\\', '/');
        if (result.contains(":")) {
            result = result.replaceAll("[^a-z0-9/.:_-]", "_");
            return new Identifier(result);
        } else {
            result = result.replaceAll("[^a-z0-9/._-]", "_");
            if (result.endsWith(".jpg") || result.endsWith(".bmp") || result.endsWith(".tga")) {
                String var10000 = result.substring(0, result.length() - 4);
                result = var10000 + ".png";
            }

            return new Identifier(baseFile.getNamespace(), FileSystems.getDefault().getPath(baseFile.getPath()).getParent().resolve(result).normalize().toString().replace('\\', '/'));
        }
    }
}
