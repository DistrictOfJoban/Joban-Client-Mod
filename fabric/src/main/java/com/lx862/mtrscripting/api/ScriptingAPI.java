package com.lx862.mtrscripting.api;

import com.lx862.mtrscripting.util.ScriptResourceUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;

/**
 * This class contains the event to hook into scripting, e.g. add new property and objects
 */
public class ScriptingAPI {
    private static final Object2ObjectArrayMap<String, String> addonVersionMap = new Object2ObjectArrayMap<>();

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
