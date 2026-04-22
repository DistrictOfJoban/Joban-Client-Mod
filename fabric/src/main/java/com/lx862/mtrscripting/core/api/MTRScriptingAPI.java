package com.lx862.mtrscripting.core.api;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.util.ScriptResourceUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * This class contains the event to hook into scripting, e.g. add new property and objects
 */
@ApiInternal
public class MTRScriptingAPI {
    private static final Map<String, String> addonVersionMap = new HashMap<>();

    /**
     * Register a version for a Mod ID, used by scripts calling {@link ScriptResourceUtil#getAddonVersion};
     * @param modId Your Mod ID
     * @param version Your version number. The format of the version string should be documented in your mod's documentation.
     */
    public static void registerAddonVersion(String modId, String version) {
        if(modId == null) throw new IllegalArgumentException("modId must not be null!");
        if(version == null) throw new IllegalArgumentException("version must not be null!");
        addonVersionMap.put(modId, version);
    }

    public static String getAddonVersion(String modId) {
        return addonVersionMap.get(modId);
    }
}
