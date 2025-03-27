package com.lx862.mtrscripting.api;

import com.lx862.mtrscripting.util.ScriptResourceUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.apache.logging.log4j.util.TriConsumer;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.Context;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.Scriptable;

/**
 * This class contains the event to hook into scripting, e.g. add new property and objects
 */
public class ScriptingAPI {
    private static final Object2ObjectArrayMap<String, String> addonVersionMap = new Object2ObjectArrayMap<>();
    private static final ObjectList<TriConsumer<String, Context, Scriptable>> onParseScriptCallback = new ObjectArrayList<>();
    private static final ObjectList<ClassRule> allowedScriptClasses = new ObjectArrayList<>();

    static {
        addClassRule(ClassRule.parse("java.awt.*"));
        addClassRule(ClassRule.parse("java.lang.*"));
        addClassRule(ClassRule.parse("java.util.*"));
        addClassRule(ClassRule.parse("sun.java2d.*"));
        addClassRule(ClassRule.parse("java.io.Closeable"));
        addClassRule(ClassRule.parse("java.io.InputStream"));
        addClassRule(ClassRule.parse("java.io.OutputStream"));
    }

    /**
     * Register a version for a Mod ID, used by scripts calling {@link ScriptResourceUtil#getAddonVersion};
     * @param modid Your Mod ID
     * @param version Your version number. The format of the version string should be documented in your mod's documentation.
     */
    public static void registerAddonVersion(String modid, String version) {
        addonVersionMap.put(modid, version);
    }

    /**
     * Register a callback that will be called when a script is to be parsed.
     * This can be to add new types/objects to the script.
     * @param callback The callback to run (Context type, Rhino Context, Scriptable)
     */
    public static void onParseScript(TriConsumer<String, Context, Scriptable> callback) {
        onParseScriptCallback.add(callback);
    }

    /**
     * By default, MTR Scripting does not allow loading any arbitrary java class for security reasons
     * Here, you can explicitly allow a class to be loaded.
     * It is compared using String.startWith, so this can also be used for allowing a whole package (e.g. java.awt)
     * Please use this wisely instead of blindly allowing classes for your convenience, we don't want a script to affect anything outside of MC.
     */
    public static void addClassRule(ClassRule classRule) {
        allowedScriptClasses.add(classRule);
    }

    public static String getAddonVersion(String modid) {
        return addonVersionMap.get(modid);
    }

    public static void callOnParseScriptCallback(String contextName, Context context, Scriptable scriptable) {
        for(TriConsumer<String, Context, Scriptable> entry : onParseScriptCallback) {
            entry.accept(contextName, context, scriptable);
        }
    }

    public static boolean isClassAllowed(String str) {
        for(ClassRule cs : allowedScriptClasses) {
            if(cs.match(str)) return true;
        }
        return false;
    }
}
