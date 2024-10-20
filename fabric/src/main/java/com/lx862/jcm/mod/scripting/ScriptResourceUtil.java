package com.lx862.jcm.mod.scripting;

/* From https://github.com/zbx1425/mtr-nte/blob/master/common/src/main/java/cn/zbx1425/mtrsteamloco/render/scripting/ScriptResourceUtil.java#L44 */

import com.lx862.jcm.mod.util.JCMLogger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mod.Keys;

import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;

@SuppressWarnings("unused")
public class ScriptResourceUtil {
    public static Context activeCtx;
    public static Scriptable activeScope;

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

    private static final FontRenderContext FONT_CONTEXT = new FontRenderContext(new AffineTransform(), true, false);

    public static FontRenderContext getFontRenderContext() {
        return FONT_CONTEXT;
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
}
