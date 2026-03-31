package com.lx862.mtrscripting.util;

/* From https://github.com/zbx1425/mtr-nte/blob/master/common/src/main/java/cn/zbx1425/mtrsteamloco/render/scripting/ScriptResourceUtil.java#L44 */

import com.lx862.mtrscripting.api.ScriptingAPI;
import com.lx862.mtrscripting.ScriptManager;

import com.lx862.mtrscripting.lib.org.mozilla.javascript.Context;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.Scriptable;
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
import java.nio.ByteBuffer;
import java.nio.file.FileSystems;
import java.text.AttributedString;
import java.util.Locale;
import java.util.Stack;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class ScriptResourceUtil {
    public static Context activeContext;
    public static Scriptable activeScope;
    private static final Stack<Identifier> scriptLocationStack = new Stack<>();

    public static void executeScript(Context rhinoCtx, Scriptable scope, Identifier scriptLocation, String script) {
        scriptLocationStack.push(scriptLocation);
        rhinoCtx.evaluateString(scope, script, scriptLocation.getNamespace() + ":" + scriptLocation.getPath(), 1, null);
        scriptLocationStack.pop();
    }

    public static void includeScript(Object pathOrIdentifier) {
        if (activeContext == null) throw new RuntimeException(
                "Cannot use include in functions, as by that time scripts are no longer processed."
        );
        Identifier identifier;
        if (pathOrIdentifier instanceof Identifier) {
            identifier = (Identifier) pathOrIdentifier;
        } else {
            identifier = idRelative(pathOrIdentifier.toString());
        }
        executeScript(activeContext, activeScope, identifier, ResourceManagerHelper.readResource(identifier));
    }

    public static void print(Object... objs) {
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
        return new Identifier(textForm);
    }

    public static Identifier id(String textForm) {
        return new Identifier(textForm);
    }

    public static Identifier idRelative(String textForm) {
        if (scriptLocationStack.empty()) throw new RuntimeException(
                "Cannot use idRelative in functions."
        );
        return resolveRelativePath(scriptLocationStack.peek(), textForm);
    }

    public static Identifier idr(String textForm) {
        if (scriptLocationStack.empty()) throw new RuntimeException(
                "Cannot use idr in functions."
        );
        Identifier id = scriptLocationStack.peek();
        return resolveRelativePath(id, textForm);
    }

    @Deprecated
    public static InputStream readStream(Identifier identifier) throws IOException {
        DataReaderJS dataReader = read(identifier);
        if(dataReader != null) {
            return dataReader.asInputStream();
        } else {
            return null;
        }
    }

    @Deprecated
    public static String readString(Identifier identifier) {
        DataReaderJS dataReader = read(identifier);
        if(dataReader == null) return null;
        return dataReader.asString();
    }

    @Deprecated
    public static BufferedImage readBufferedImage(Identifier id) {
        DataReaderJS dataReader = read(id);
        if(dataReader == null) return null;
        return dataReader.asBufferedImage();
    }

    @Deprecated
    public static Font readFont(Identifier id) {
        DataReaderJS dataReader = read(id);
        if(dataReader == null) return null;
        return dataReader.asFont();
    }

    public static DataReaderJS read(Identifier identifier) {
        // HACK: MC Mappings always uses a callback for the InputStream and auto-closes it after, so we can't pass it around later on
        // For now, we just save the full file bytes, then wrap it in another ByteArrayInputStream...
        byte[][] fileBytes = new byte[][]{null};
        ResourceManagerHelper.readResource(identifier, is -> {
            try {
                fileBytes[0] = is.readAllBytes();
            } catch (IOException e) {
                ScriptManager.LOGGER.error("[JCM Scripting] Error while reading data {}", identifier);
            }
        });

        if(fileBytes[0] != null) {
            return new DataReaderJS(() -> new ByteArrayInputStream(fileBytes[0]));
        } else {
            return null;
        }
    }

    private static final Identifier NOTO_SANS_CJK_LOCATION = new Identifier("mtr", "font/noto-sans-cjk-tc-medium.otf");
    private static final Identifier NOTO_SANS_LOCATION = new Identifier("mtr", "font/noto-sans-semibold.ttf");
    private static final Identifier NOTO_SERIF_LOCATION = new Identifier("mtr", "font/noto-serif-cjk-tc-semibold.ttf");
    private static Font NOTO_SANS_CACHE;
    private static Font NOTO_SANS_CJK_CACHE;
    private static Font NOTO_SERIF_CACHE;

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
                    ScriptManager.LOGGER.error("[JCM Scripting] Failed loading font", ex);
                    return null;
                }
            }
            return NOTO_SERIF_CACHE;
        } else if(fontName.equals("Noto Sans CJK TC Medium")) {
            if(NOTO_SANS_CJK_CACHE == null) {
                try {
                    NOTO_SANS_CJK_CACHE = readFont(NOTO_SANS_CJK_LOCATION);
                } catch (Exception ex) {
                    ScriptManager.LOGGER.error("[JCM Scripting] Failed to load font", ex);
                }
            }
            return NOTO_SANS_CJK_CACHE;
        } else if(fontName.equals("Noto Sans SemiBold")) {
            if(NOTO_SANS_CACHE == null) {
                try {
                    NOTO_SANS_CACHE = readFont(NOTO_SANS_LOCATION);
                } catch (Exception ex) {
                    ScriptManager.LOGGER.error("[JCM Scripting] Failed to load font", ex);
                }
            }
            return NOTO_SANS_CACHE;
        } else {
            return new Font(fontName, Font.PLAIN, 1);
        }
    }

    private static final FontRenderContext FONT_CONTEXT = new FontRenderContext(new AffineTransform(), true, false);

    public static FontRenderContext getFontRenderContext() {
        return FONT_CONTEXT;
    }

    public static AttributedString ensureStrFonts(String text, Font font) {
        AttributedString result = new AttributedString(text);
        if (text.isEmpty()) return result;
        result.addAttribute(TextAttribute.FONT, font, 0, text.length());
        for (int characterIndex = 0; characterIndex < text.length(); characterIndex++) {
            final char character = text.charAt(characterIndex);
            if (!font.canDisplay(character)) {
                Font defaultFont = null;
                for (final Font testFont : GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts()) {
                    if (testFont.canDisplay(character)) {
                        defaultFont = testFont;
                        break;
                    }
                }
                final Font newFont = (defaultFont == null ? new Font(null) : defaultFont)
                        .deriveFont(font.getStyle(), font.getSize2D());
                result.addAttribute(TextAttribute.FONT, newFont, characterIndex, characterIndex + 1);
            }
        }
        return result;
    }

    public static String getAddonVersion(String modid) {
        return ScriptingAPI.getAddonVersion(modid);
    }

    @Deprecated
    public static String getMTRVersion() {
        return getAddonVersion("mtr"); // This assumes MTR have registered the version
    }

    @Deprecated
    public static String getNTEVersion() {
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
