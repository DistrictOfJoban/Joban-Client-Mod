package com.lx862.jcm.mod.scripting;

/* From https://github.com/zbx1425/mtr-nte/blob/master/common/src/main/java/cn/zbx1425/mtrsteamloco/render/scripting/ScriptResourceUtil.java#L44 */

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.util.JCMLogger;

import vendor.com.lx862.jcm.org.mozilla.javascript.Context;
import vendor.com.lx862.jcm.org.mozilla.javascript.Scriptable;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.ResourceManagerHelper;
import org.mtr.mod.Keys;

import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.Locale;
import java.util.Stack;

@SuppressWarnings("unused")
public class ScriptResourceUtil {
    public static Context activeContext;
    public static Scriptable activeScope;
    private static final Stack<Identifier> scriptLocationStack = new Stack<>();

    public static void executeScript(Context rhinoCtx, Scriptable scope, Identifier scriptLocation, String script) {
        scriptLocationStack.push(scriptLocation);
        rhinoCtx.evaluateString(scope, script, scriptLocation.toString(), 1, null);
        scriptLocationStack.pop();
    }

    public static void includeScript(Object pathOrIdentifier) throws IOException {
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
        for(Object obj : objs) {
            sb.append(obj.toString());
        }
        JCMLogger.info("[Scripting] {}", sb.toString().trim());
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

    private static final FontRenderContext FONT_CONTEXT = new FontRenderContext(new AffineTransform(), true, false);

    public static FontRenderContext getFontRenderContext() {
        return FONT_CONTEXT;
    }

    public static String getJCMVersion() {
        return Constants.MOD_VERSION;
    }

    public static String getNTEVersion() { // Hardcoded for backward compat
        return "0.5.2+1.19.2";
    }

    public static int getNTEVersionInt() { // Hardcoded for backward compat
        return 502;
    }

    public static int getNTEProtoVersion() { // Hardcoded for backward compat
        return 2;
    }

    public static String getMTRVersion() {
        String mtrModVersion;
        try {
            mtrModVersion = (String) Keys.class.getField("MOD_VERSION").get(null);
        } catch (ReflectiveOperationException ignored) {
            mtrModVersion = null;
        }
        return mtrModVersion;
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
