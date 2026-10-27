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
    private static final Map<String, ScriptingLanguage> supportedLanguages = new HashMap<>();

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

    /**
     * Register a programming language.
     * @param languageName The language name to be referenced
     * @param scriptingLanguage The properties of the scripting language
     */
    public static void addScriptingLanguage(String languageName, ScriptingLanguage scriptingLanguage) {
        supportedLanguages.put(languageName, scriptingLanguage);
    }

    public static String getAddonVersion(String modId) {
        return addonVersionMap.get(modId);
    }

    public static ScriptingLanguage getScriptingLanguage(String languageName) {
        return supportedLanguages.get(languageName);
    }
}
