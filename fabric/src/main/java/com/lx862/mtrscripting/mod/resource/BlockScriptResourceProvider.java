package com.lx862.mtrscripting.mod.resource;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lx862.jcm.nativeapi.NativeScriptManager;
import com.lx862.mtrscripting.mod.MTRScriptingModClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Parses the three ABI 16 block-script categories of
 * {@code mtr_custom_resources.json} — native (C++) modules anchored on
 * FangSu blocks through the client-side block-script bindings (edited
 * with the PSD Script Editor item on a level crossing / ticket machine /
 * announcement speaker block).
 *
 * <h2>Entry shape (native declarations only, same conventions as the
 * other categories)</h2>
 * <pre>{@code
 * "levelCrossingScripts": [
 *     {
 *         "id": "demo:crossing_barrier",
 *         "scriptLocations": ["demo:natives/crossing_barrier"],
 *         "input": { "openSpeed": 2.5 }
 *     }
 * ],
 * "fareMachineScripts":   [ ... ],
 * "announcementScripts":  [ ... ]
 * }</pre>
 *
 * <p>These kinds have no JavaScript counterpart — a {@code .js} entry in
 * the list is warned about and skipped (the C++ classification rules of
 * {@code scriptLocations} are the same as everywhere: stems, full
 * library paths, or per-platform maps). Entries are picked per-block by
 * the player, never referenced from other resource entries, so every
 * declaration is always parsed.</p>
 */
public class BlockScriptResourceProvider implements ScriptResourceProvider {

    public enum Kind {
        LEVEL_CROSSING("levelCrossingScripts", "level_crossing", NativeScriptManager.MTR_RESOURCE_LEVEL_CROSSING),
        FARE_MACHINE("fareMachineScripts", "fare_machine", NativeScriptManager.MTR_RESOURCE_FARE_MACHINE),
        ANNOUNCEMENT("announcementScripts", "announcement", NativeScriptManager.MTR_RESOURCE_ANNOUNCEMENT);

        public final String jsonKey;
        public final String typeName;
        public final int resourceKind;

        Kind(String jsonKey, String typeName, int resourceKind) {
            this.jsonKey = jsonKey;
            this.typeName = typeName;
            this.resourceKind = resourceKind;
        }
    }

    private final Map<String, String> scripts = new HashMap<>();

    @Override
    public void parseCustom() {
    }

    @Override
    public void parseMtrResources(JsonObject rootObject, boolean isLegacyResource) {
        if (isLegacyResource) return; // MTR 3 packs predate the ABI 16 kinds

        for (Kind kind : Kind.values()) {
            final JsonElement element = rootObject.get(kind.jsonKey);
            if (element == null || !element.isJsonArray()) continue;
            final JsonArray array = element.getAsJsonArray();
            for (JsonElement entryElement : array) {
                try {
                    final JsonObject scriptObject = entryElement.getAsJsonObject();
                    final String id = scriptObject.get("id").getAsString();
                    if (id.isEmpty() || id.length() > 256) {
                        throw new IllegalArgumentException("Block script id must contain 1..256 characters");
                    }
                    /* Reuse the shared entry parser for the native
                       declaration forms (scriptLocations classification,
                       legacy nativeLibrary fields, the input JSON). */
                    ScriptResourceProvider.tryParseScript(id, kind.typeName, kind.typeName, scriptObject, true, false);
                    if (NativeScriptManager.isLoaded(id)) {
                        scripts.put(id, kind.typeName);
                    } else {
                        MTRScriptingModClient.LOGGER.warn("[MTR Scripting via JCM] {} script \"{}\" has no loaded C++ module — entry skipped! (These kinds are native-only.)", kind.typeName, id);
                    }
                } catch (Exception e) {
                    ScriptResourceProvider.logError("parsing " + kind.typeName + " script entry '" + VehicleResourceProvider.safeId(entryElement) + "' in mtr_custom_resources.json (entry skipped)", e);
                }
            }
        }
    }

    @Override
    public void validate() {
        for (Map.Entry<String, String> entry : scripts.entrySet()) {
            if (NativeScriptManager.isSkipped(entry.getKey())) {
                MTRScriptingModClient.LOGGER.warn("[MTR Scripting via JCM] {} native script \"{}\" is skipped on this platform ({}) — see the earlier nativeLibrary hint.",
                        entry.getValue(), entry.getKey(), NativeScriptManager.getPlatformKeyExact());
            }
        }
    }

    @Override
    public void reset() {
        scripts.clear();
    }

    /** All loaded script ids of one kind, sorted (the editor GUI list). */
    public List<String> getScriptsOfType(String typeName) {
        final List<String> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : scripts.entrySet()) {
            if (entry.getValue().equals(typeName)) result.add(entry.getKey());
        }
        result.sort(String::compareTo);
        return result;
    }

    public boolean isEmpty() {
        return scripts.isEmpty();
    }

    public boolean isLoaded(String id) {
        return scripts.containsKey(id) && NativeScriptManager.isLoaded(id);
    }

    /** The kind matching a script id, or null. */
    public static String typeNameOf(int resourceKind) {
        switch (resourceKind) {
            case NativeScriptManager.MTR_RESOURCE_LEVEL_CROSSING: return Kind.LEVEL_CROSSING.typeName;
            case NativeScriptManager.MTR_RESOURCE_FARE_MACHINE: return Kind.FARE_MACHINE.typeName;
            case NativeScriptManager.MTR_RESOURCE_ANNOUNCEMENT: return Kind.ANNOUNCEMENT.typeName;
            default: return null;
        }
    }
}
