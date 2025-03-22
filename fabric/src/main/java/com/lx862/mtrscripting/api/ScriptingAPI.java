package com.lx862.mtrscripting.api;

import com.lx862.mtrscripting.scripting.util.ScriptResourceUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.apache.logging.log4j.util.TriConsumer;
import vendor.com.lx862.jcm.org.mozilla.javascript.Context;
import vendor.com.lx862.jcm.org.mozilla.javascript.Scriptable;

/**
 * This class contains the event to hook into scripting, e.g. add new property and objects
 */
public class ScriptingAPI {
    private static final Object2ObjectArrayMap<String, String> addonVersionMap = new Object2ObjectArrayMap<>();
    private static final ObjectList<TriConsumer<String, Context, Scriptable>> onParseScriptCallback = new ObjectArrayList<>();
    private static final ObjectList<String> allowedScriptClasses = new ObjectArrayList<>();
    private static final ObjectList<String> deniedScriptClasses = new ObjectArrayList<>();

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
    public static void allowClass(String className) {
        allowedScriptClasses.add(className);
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
        for(String cs : allowedScriptClasses) {
            if(str.startsWith(cs)) return true;
        }
        System.out.println("Blocked " + str);
        return false;
    }
}
