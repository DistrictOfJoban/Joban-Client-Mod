package com.lx862.mtrscripting.mod.resource;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lx862.jcm.mod.scripting.JCMScripting;
import com.lx862.jcm.mod.scripting.psd.PSDBinding;
import com.lx862.jcm.mod.scripting.psd.PSDWrapper;
import com.lx862.jcm.nativeapi.NativeScriptManager;
import com.lx862.mtrscripting.core.primitive.ParsedScript;
import com.lx862.mtrscripting.mod.MTRScriptingModClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Parses the {@code psdScripts} category of {@code mtr_custom_resources.json}
 * — scripts that can be bound to platform screen door glass or top panels with
 * the "PSD Script Editor" (屏蔽门脚本编辑器) item.
 *
 * <h2>Entry shape (same file conventions as {@code vehicleScripts})</h2>
 * <pre>{@code
 * "psdScripts": [
 *     {
 *         "id": "psd_glass_pids",
 *         "type": "glass",                       // "glass" | "top" (required choice)
 *         "scriptLocations": [
 *             "mtr:my_pack/js/psd_glass_pids/main.js",
 *             "mtr:my_pack/native/psd_glass_pids"
 *         ],
 *         "dataFetchMode": "ALL"                 // SKIP | MANDATORY | ALL (default SKIP)
 *     }
 * ]
 * }</pre>
 *
 * <p>Every {@code scriptLocations} entry is classified exactly like the vehicle
 * category ({@code *.js} is JavaScript, anything else is a C++ module — stem,
 * full library path, or per-platform map); JS and C++ may be freely mixed under
 * one id, and the legacy {@code language}/{@code nativeLibrary}/
 * {@code nativeLibraries} fields keep working. The {@code type} field selects
 * which binding surface the entry shows up on in the editor GUI: {@code glass}
 * for PSD glass blocks, {@code top} for the top panel above the doors.</p>
 *
 * <p>Entries are not referenced from other resource entries — they are picked
 * per-block by the player — so (unlike vehicles) every entry is always parsed;
 * {@code dataFetchMode} gates the heavy route-stop data inside the wrapper
 * (SKIP: route identity only; MANDATORY/ALL: full per-route stop lists with
 * interchange and exit info).</p>
 */
public class PSDResourceProvider implements ScriptResourceProvider {
    private final Map<String, PSDScriptConfiguration> psdScripts = new HashMap<>();

    @Override
    public void parseCustom() {
    }

    @Override
    public void parseMtrResources(JsonObject rootObject, boolean isLegacyResource) {
        if (isLegacyResource) return; // MTR 3 packs have no PSD scripting

        final JsonElement psdScriptsElement = rootObject.get("psdScripts");
        if (psdScriptsElement == null || !psdScriptsElement.isJsonArray()) return;

        final JsonArray scriptArray = psdScriptsElement.getAsJsonArray();
        for (JsonElement entryElement : scriptArray) {
            try {
                final JsonObject scriptObject = entryElement.getAsJsonObject();
                final String scriptEntryId = scriptObject.get("id").getAsString();
                if (scriptEntryId.isEmpty() || scriptEntryId.length() > PSDBinding.MAX_SCRIPT_ID_LENGTH) {
                    throw new IllegalArgumentException("PSD script id must contain 1.." + PSDBinding.MAX_SCRIPT_ID_LENGTH + " characters");
                }

                PSDWrapper.PSDType type;
                if (scriptObject.has("type")) {
                    final String typeString = scriptObject.get("type").getAsString();
                    try {
                        type = PSDWrapper.PSDType.valueOf(typeString.toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException invalidType) {
                        MTRScriptingModClient.LOGGER.warn("[MTR Scripting via JCM] PSD script \"{}\" has invalid type \"{}\" (expected \"glass\" or \"top\") — entry skipped!", scriptEntryId, typeString);
                        continue;
                    }
                } else {
                    MTRScriptingModClient.LOGGER.warn("[MTR Scripting via JCM] PSD script \"{}\" is missing the \"type\" field (\"glass\" or \"top\") — defaulting to \"glass\".", scriptEntryId);
                    type = PSDWrapper.PSDType.GLASS;
                }

                final PSDWrapper.DataFetchMode dataFetchMode;
                if (scriptObject.has("dataFetchMode")) {
                    try {
                        dataFetchMode = PSDWrapper.DataFetchMode.valueOf(scriptObject.get("dataFetchMode").getAsString());
                    } catch (IllegalArgumentException invalidMode) {
                        throw new IllegalArgumentException("Unknown dataFetchMode \"" + scriptObject.get("dataFetchMode").getAsString() + "\" (expected SKIP, MANDATORY or ALL)");
                    }
                } else {
                    dataFetchMode = PSDWrapper.DataFetchMode.SKIP;
                }

                /* PSD scripts draw with the jcm PIDS wrappers (Text/Texture/
                   Rect), so they parse under the JCM script manager — the
                   same manager the scripted PIDS presets use. C++ modules
                   load through NativeScriptManager exactly like vehicles. */
                final ParsedScript parsedScript = ScriptResourceProvider.tryParseScript(scriptEntryId, "psd", "psd", scriptObject, true, false, JCMScripting.getScriptManager());
                if (parsedScript != null || NativeScriptManager.isLoaded(scriptEntryId)) {
                    psdScripts.put(scriptEntryId, new PSDScriptConfiguration(scriptEntryId, parsedScript, type, dataFetchMode));
                } else {
                    MTRScriptingModClient.LOGGER.warn("[MTR Scripting via JCM] PSD script \"{}\" has neither a readable JavaScript part nor a loaded C++ module — entry skipped!", scriptEntryId);
                }
            } catch (Exception e) {
                ScriptResourceProvider.logError("parsing PSD script entry '" + VehicleResourceProvider.safeId(entryElement) + "' in mtr_custom_resources.json (entry skipped)", e);
            }
        }
    }

    @Override
    public void validate() {
        for (Map.Entry<String, PSDScriptConfiguration> entry : psdScripts.entrySet()) {
            final PSDScriptConfiguration config = entry.getValue();
            if (config.parsedScript() == null && NativeScriptManager.isSkipped(entry.getKey())) {
                MTRScriptingModClient.LOGGER.warn("[MTR Scripting via JCM] PSD native script \"{}\" is skipped on this platform ({}) — see the earlier nativeLibrary hint.", entry.getKey(), NativeScriptManager.getPlatformKeyExact());
            }
        }
    }

    @Override
    public void reset() {
        psdScripts.clear();
    }

    public PSDScriptConfiguration getScript(String scriptEntryId) {
        return psdScripts.get(scriptEntryId);
    }

    /** All entries of one binding surface, sorted by id — the editor GUI list. */
    public List<PSDScriptConfiguration> getScriptsOfType(PSDWrapper.PSDType type) {
        final List<PSDScriptConfiguration> result = new ArrayList<>();
        for (PSDScriptConfiguration config : psdScripts.values()) {
            if (config.type() == type) result.add(config);
        }
        result.sort((a, b) -> a.id().compareTo(b.id()));
        return result;
    }

    public boolean isEmpty() {
        return psdScripts.isEmpty();
    }

    public record PSDScriptConfiguration(String id, ParsedScript parsedScript, PSDWrapper.PSDType type, PSDWrapper.DataFetchMode dataFetchMode) {
        public boolean hasNative() {
            return NativeScriptManager.isLoaded(id);
        }
    }
}
