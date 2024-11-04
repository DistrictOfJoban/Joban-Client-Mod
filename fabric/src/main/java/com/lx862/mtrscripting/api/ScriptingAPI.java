package com.lx862.mtrscripting.api;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.apache.logging.log4j.util.TriConsumer;
import vendor.com.lx862.jcm.org.mozilla.javascript.Context;
import vendor.com.lx862.jcm.org.mozilla.javascript.Scriptable;

/**
 * This class contains the event to hook into scripting, i.e. add new property and objects
 */
public class ScriptingAPI {
    private static final Object2ObjectArrayMap<String, String> addonVersionMap = new Object2ObjectArrayMap<>();
    private static final ObjectList<TriConsumer<String, Context, Scriptable>> onParseScriptCallback = new ObjectArrayList<>();

    /**
     * Register a version for a Mod ID, used by scripts for {@link com.lx862.mtrscripting.scripting.ScriptResourceUtil#getAddonVersion};
     * @param modid Your Mod ID
     * @param version Your version number. The format of the version string should be mentioned in your mod's documentation.
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

    public static String getAddonVersion(String modid) {
        return addonVersionMap.get(modid);
    }

    public static void callOnParseScriptCallback(String contextName, Context context, Scriptable scriptable) {
        for(TriConsumer<String, Context, Scriptable> entry : onParseScriptCallback) {
            entry.accept(contextName, context, scriptable);
        }
    }
}
