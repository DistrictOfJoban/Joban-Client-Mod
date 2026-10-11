package com.lx862.mtrscripting.mod.resource;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lx862.jcm.mod.config.JCMClientConfig;
import com.lx862.jcm.nativeapi.NativeScriptManager;
import com.lx862.mtrscripting.core.primitive.ParsedScript;
import com.lx862.mtrscripting.core.primitive.ScriptContent;
import com.lx862.mtrscripting.mod.MTRScriptingMod;
import com.lx862.mtrscripting.mod.MTRScriptingModClient;
import com.lx862.mtrscripting.mod.impl.mtr.MTRContentScripting;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ResourceManagerHelper;
import org.mtr.mapping.mapper.TextHelper;

import java.util.List;

public interface ScriptResourceProvider {

    void parseCustom();

    void parseMtrResources(JsonObject rootObject, boolean isLegacyResource);

    void validate();

    void reset();

    static ParsedScript tryParseScript(String id, String scriptType, String contextName, JsonObject jsonObject, boolean isParsingMTR4, boolean useSnakeCase) {
        /* Yanyang: C++ native script entries bypass Rhino entirely.
           The running OS/arch is auto-detected, the per-platform
           nativeLibrary declaration is resolved, and the module is
           loaded (or the script skipped with a latest.log + debug-mode
           hint when this platform isn't declared). Returns null either
           way: rendering for these ids dispatches through
           NativeScriptManager, not the JS instance pipeline.

           MULTI-MODULE: one script id may declare several libraries —
           either "nativeLibraries": [..] (Yanyang extension) or the
           single "nativeLibrary" field, which is treated as a
           one-element list. All of them run for the same id; this is how
           our resource pack keeps main.js and overlay.js together the way
           the JS route did with two scriptLocations entries. */
        if (isNativeCppScript(jsonObject)) {
            final JsonElement many = jsonObject.get(nativeLibrariesKey(useSnakeCase));
            final JsonElement declared = (many != null && !many.isJsonNull())
                    ? many
                    : jsonObject.get(nativeLibraryKey(useSnakeCase));
            NativeScriptManager.configure(id, jsonObject.get(isParsingMTR4 ? "input" : "scriptInput"), null);
            NativeScriptManager.loadManyFromDeclaration(id, declared);
            return null;
        }

        final List<ScriptContent> scripts = new ObjectArrayList<>();
        final String scriptFilesKey = isParsingMTR4 ? "scriptLocations" : useSnakeCase ? "script_files" : "scriptFiles";
        final String scriptTextsKey = isParsingMTR4 ? "prependExpressions" : useSnakeCase ? "script_texts" : "scriptTexts";
        final String scriptInputKey = isParsingMTR4 ? "input" : "scriptInput";

        if (jsonObject.has(scriptFilesKey) || jsonObject.has(scriptTextsKey)) {
            // Parse script input and pass to the script
            if(jsonObject.has(scriptInputKey)) {
                String str = jsonObject.get(scriptInputKey).toString();
                Identifier scriptLocationSource = MTRScriptingMod.id("internal/script_input/" + scriptType + "/" + id);
                scripts.add(new ScriptContent(scriptLocationSource, "const SCRIPT_INPUT = " + str + ";"));
            }

            if(jsonObject.has(scriptTextsKey)) {
                JsonArray scriptTextArray = jsonObject.get(scriptTextsKey).getAsJsonArray();
                for(int i = 0; i < scriptTextArray.size(); i++) {
                    Identifier scriptLocationSource = MTRScriptingMod.id("internal/script_texts/" + scriptType + "/" + id + "/line" + i);
                    String scriptText = scriptTextArray.get(i).getAsString();
                    scripts.add(new ScriptContent(scriptLocationSource, scriptText));
                }
            }

            if(jsonObject.has(scriptFilesKey)) {
                JsonArray scriptFilesArray = jsonObject.get(scriptFilesKey).getAsJsonArray();
                for(int i = 0; i < scriptFilesArray.size(); i++) {
                    Identifier scriptLocationSource = new Identifier(scriptFilesArray.get(i).getAsString());
                    String scriptText = ResourceManagerHelper.readResource(scriptLocationSource);
                    if(scriptText.isEmpty()) {
                        MTRScriptingModClient.LOGGER.warn("[MTR Scripting via JCM] Script {}:{} is missing (or empty)!", scriptLocationSource.getNamespace(), scriptLocationSource.getPath());
                        continue;
                    }

                    scripts.add(new ScriptContent(scriptLocationSource, scriptText));
                }
            }
        }

        try {
            return scripts.isEmpty() ? null : MTRContentScripting.getScriptManager().parseScript(id + " (" + scriptType + ")", contextName, scripts, JCMClientConfig.INSTANCE.scripting.scriptDebugMode.value());
        } catch (Exception e) {
            ScriptResourceProvider.logError("parsing " + scriptType + " script (" + id + ")", e);
            return null;
        }
    }

    /** True when the entry declares "language": "cpp" (native module). */
    static boolean isNativeCppScript(JsonObject jsonObject) {
        final JsonElement language = jsonObject.get("language");
        return language != null && language.isJsonPrimitive() && "cpp".equalsIgnoreCase(language.getAsString());
    }

    /** nativeLibrary field name: "nativeLibrary" (MTR 4 / NTE) or
        "native_library" (snake_case legacy entries). */
    static String nativeLibraryKey(boolean useSnakeCase) {
        return useSnakeCase ? "native_library" : "nativeLibrary";
    }

    /** Multi-library field name: "nativeLibraries" (MTR 4) or
        "native_libraries" (snake_case legacy entries).
        Yanyang extension: MTR's vehicle schema carries ONE scriptId, so a
        pack that needs several C++ libraries for one train lists them here
        and they all run under that id, in declaration order. */
    static String nativeLibrariesKey(boolean useSnakeCase) {
        return useSnakeCase ? "native_libraries" : "nativeLibraries";
    }

    static void logError(String action, Exception e) {
        if(JCMClientConfig.INSTANCE.scripting.scriptDebugMode.value()) {
            MTRScriptingModClient.LOGGER.error("[MTR Scripting] Error while {}!", action, e);
            MTRContentScripting.getScriptManager().scriptErrorNotifier.queue(() -> {
                MinecraftClient.getInstance().getPlayerMapped().sendMessage(Text.cast(TextHelper.setStyle(TextHelper.literal("[MTR] Error while " + action + "!"), Style.getEmptyMapped().withColor(TextFormatting.RED))), false);
                MinecraftClient.getInstance().getPlayerMapped().sendMessage(Text.cast(TextHelper.setStyle(TextHelper.literal("See Console for details."), Style.getEmptyMapped().withColor(TextFormatting.RED))), false);
            });
        } else {
            MTRScriptingModClient.LOGGER.error("[MTR Scripting] Error while {}: {}", action, e.getMessage());
            MTRScriptingModClient.LOGGER.error("(Enable debug mode to see more information)");
        }
    }
}
