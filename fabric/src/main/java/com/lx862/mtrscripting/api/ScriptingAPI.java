package com.lx862.mtrscripting.api;

import com.lx862.mtrscripting.ScriptManager;
import com.lx862.mtrscripting.util.ScriptResourceUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.apache.logging.log4j.util.TriConsumer;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.Context;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.Scriptable;

import java.util.concurrent.ExecutorService;

/**
 * This class contains the event to hook into scripting, e.g. add new property and objects
 */
public class ScriptingAPI {
    private static final Object2ObjectArrayMap<String, String> addonVersionMap = new Object2ObjectArrayMap<>();

    /**
     * Create a new script manager.
     * @param thread An executor service, where script tasks will be submitted.
     */
    public static ScriptManager createScriptManager() {
        return new ScriptManager();
    }

    /**
     * Register a version for a Mod ID, used by scripts calling {@link ScriptResourceUtil#getAddonVersion};
     * @param modid Your Mod ID
     * @param version Your version number. The format of the version string should be documented in your mod's documentation.
     */
    public static void registerAddonVersion(String modid, String version) {
        addonVersionMap.put(modid, version);
    }

    public static String getAddonVersion(String modid) {
        return addonVersionMap.get(modid);
    }
}
